package com.azjvsdk.experimental.http.pipeline;

import com.azjvsdk.experimental.http.HttpClient;
import com.azjvsdk.experimental.http.HttpRequest;
import com.azjvsdk.experimental.http.HttpResponse;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The http pipeline.
 */
public final class HttpPipeline {
    private final PolicyEntry[] requestPolicyEntries;
    private final HttpClient httpClient;

    /**
     * Creates a HttpPipeline holding array of global policies that gets applied
     * to all request initiated through {@link HttpPipeline#sendRequest(PipelineCallContext)}
     * and it's response.
     *
     * @param requestPolicies request policies in the order they need to applied
     * @param httpClient the http client to write request to wire and receive response from wire.
     */
    public HttpPipeline(RequestPolicy[] requestPolicies, HttpClient httpClient) {
        Objects.requireNonNull(requestPolicies);
        Objects.requireNonNull(httpClient);
        this.requestPolicyEntries = new PolicyEntry[requestPolicies.length];
        for (int i = 0; i < requestPolicies.length; i++) {
            Objects.requireNonNull(requestPolicies[i]);
            this.requestPolicyEntries[i] = new PolicyEntry(requestPolicies[i].getClass().getName(), requestPolicies[i]);
        }
        this.httpClient = httpClient;
    }

    /**
     * Creates a HttpPipeline holding array of global policies that gets applied
     * to all request initiated through {@link HttpPipeline#sendRequest(PipelineCallContext)}
     * and it's response.
     *
     * @param requestPolicyEntries request policy entries, each entry contains policy name and
     *                             request policy. The policies get applied in the order of entries array.
     * @param httpClient the http client to write request to wire and receive response from wire.
     */
    public HttpPipeline(PolicyEntry[] requestPolicyEntries, HttpClient httpClient) {
        Objects.requireNonNull(requestPolicyEntries);
        Objects.requireNonNull(httpClient);
        this.requestPolicyEntries = requestPolicyEntries;
        this.httpClient = httpClient;
    }

    /**
     * @return global request policy entries in the pipeline.
     */
    public PolicyEntry[] requestPolicyEntries() {
        return this.requestPolicyEntries;
    }

    /**
     * Creates a new context local to the provided http request.
     *
     * @param httpRequest the request for a context needs to be created
     * @return the request context
     */
    public PipelineCallContext newContext(HttpRequest httpRequest) {
        return new PipelineCallContext(this.httpClient, httpRequest, this.requestPolicyEntries);
    }

    /**
     * Creates a new patched HttpPipeline from the this HttpPipeline.
     *
     * The provided {@code requestPolicyPatchEntries} are applied as follows:
     *  A policy entry with {@code null} policy value will be remove an existing policy with matching name.
     *  A policy entry with non-null policy value will
     *      a. replace an existing policy with matching name
     *      b. if there is no existing policy with matching name then policy entry will be appended to the current policies.
     *
     * @param requestPolicyPatchEntries entries to patch
     * @return patched HttpPipeline
     */
    public HttpPipeline newPatchedPipeline(PolicyEntry[] requestPolicyPatchEntries) {
        Map<String, RequestPolicy> map = new HashMap<String, RequestPolicy>();
        for (int i = 0; i < requestPolicyPatchEntries.length; i++) {
            map.put(requestPolicyPatchEntries[i].name().toLowerCase(), requestPolicyPatchEntries[i].policy());
        }
        return newPatchedPipeline(new PatchPolicies() {
            PolicyEntry patches[] = new PolicyEntry[1];
            //
            @Override
            public PatchedPolicy onNext(String name, RequestPolicy policy) {
                final String nameLower = name.toLowerCase();
                if (map.containsKey(nameLower)) {
                    RequestPolicy patch = map.get(nameLower);
                    map.remove(nameLower);
                    if (patch == null) {
                        return new PatchedPolicy(PolicyPatchAction.REMOVE, null, null);
                    } else {
                        patches[0] = new PolicyEntry(name, patch);
                        return new PatchedPolicy(PolicyPatchAction.REPLACE, name, patches);
                    }
                }
                return PatchedPolicy.NOP;
            }

            @Override
            public PolicyEntry[] tailEntries() {
                if (map.size() == 0) {
                    return null;
                } else {
                    List<PolicyEntry> tailEntries = new ArrayList<PolicyEntry>();
                    for (int i = 0; i < requestPolicyPatchEntries.length; i++) {
                        if (!map.containsKey(requestPolicyPatchEntries[i].name().toLowerCase())) {
                            tailEntries.add(requestPolicyPatchEntries[i]);
                        }
                    }
                    return tailEntries.toArray(new PolicyEntry[0]);
                }
            }
        });
    }

    /**
     * Creates a new patched HttpPipeline from the this HttpPipeline.
     *
     * @param patchCallback the callback to inspect and patch policies in the current pipeline,
     *                      to add policies to the front and end.
     * @return patched HttpPipeline
     */
    public HttpPipeline newPatchedPipeline(PatchPolicies patchCallback) {
        Objects.requireNonNull(patchCallback);
        int size = requestPolicyEntries.length;
        Set<String> seenNames = new HashSet<String>();
        PolicyRef firstPolicyRef = null;
        if (size > 0) {
            firstPolicyRef = new PolicyRef(requestPolicyEntries[0]);
            seenNames.add(firstPolicyRef.name.toLowerCase());
            PolicyRef ref = firstPolicyRef;
            for (int i = 1; i < size; i++) {
                PolicyEntry entry = requestPolicyEntries[i];
                PolicyRef newItem = new PolicyRef(entry);
                seenNames.add(newItem.name.toLowerCase());
                PolicyRef.addAfter(newItem, ref);
                ref = newItem;
            }
        }
        //
        do {
            PolicyRef current = firstPolicyRef;
            while (current != null) {
                PatchedPolicy r = patchCallback.onNext(current.name, current.policy);
                //
                Objects.requireNonNull(r);
                Objects.requireNonNull(r.action);
                if (r.action == PolicyPatchAction.NOP) {
                    current = current.next;
                    continue;
                } else if (r.action == PolicyPatchAction.ADD_AFTER) {
                    Objects.requireNonNull(r.policyEntries);
                    //
                    PolicyRef ref = current;
                    for (int i = 0; i < r.policyEntries.length; i++) {
                        PolicyEntry entry = r.policyEntries[i];
                        Objects.requireNonNull(entry);
                        Objects.requireNonNull(entry.name());
                        Objects.requireNonNull(entry.policy());
                        //
                        if (!seenNames.add(entry.name().toLowerCase())) {
                            patchCallback.onError(PolicyPatchFailed.NAME_CONFLICT, entry.name());
                        } else {
                            PolicyRef newPolicy = new PolicyRef(entry.name(), entry.policy());
                            PolicyRef.addAfter(newPolicy, ref);
                            ref = newPolicy;
                        }
                    }
                    current = ref.next;
                    continue;
                } else if (r.action == PolicyPatchAction.ADD_BEFORE) {
                    Objects.requireNonNull(r.policyEntries);
                    //
                    PolicyRef first = null;
                    PolicyRef ref = null;
                    for (int i = 0; i < r.policyEntries.length; i++) {
                        PolicyEntry entry = r.policyEntries[i];
                        Objects.requireNonNull(entry);
                        Objects.requireNonNull(entry.name());
                        Objects.requireNonNull(entry.policy());
                        //
                        if (first == null) {
                            if (!seenNames.add(entry.name().toLowerCase())) {
                                patchCallback.onError(PolicyPatchFailed.NAME_CONFLICT, entry.name());
                            } else {
                                first = new PolicyRef(entry.name(), entry.policy());
                                ref = first;
                            }
                        } else {
                            if (!seenNames.add(entry.name().toLowerCase())) {
                                patchCallback.onError(PolicyPatchFailed.NAME_CONFLICT, entry.name());
                            } else {
                                PolicyRef newPolicy = new PolicyRef(entry.name(), entry.policy());
                                PolicyRef.addAfter(newPolicy, ref);
                                ref = newPolicy;
                            }
                        }
                    }
                    if (ref != null) {
                        ref.next = current;
                        current.previous  = ref;
                    }
                    if (current == firstPolicyRef && first != null) {
                        firstPolicyRef = first;
                    }
                    current = current.next;
                    continue;
                } else if (r.action == PolicyPatchAction.REPLACE) {
                    Objects.requireNonNull(r.policyEntries);
                    //
                    PolicyRef first = null;
                    PolicyRef ref = null;
                    for (int i = 0; i < r.policyEntries.length; i++) {
                        PolicyEntry entry = r.policyEntries[i];
                        Objects.requireNonNull(entry);
                        Objects.requireNonNull(entry.name());
                        Objects.requireNonNull(entry.policy());
                        //
                        if (first == null) {
                            if (!seenNames.add(entry.name().toLowerCase())) {
                                patchCallback.onError(PolicyPatchFailed.NAME_CONFLICT, entry.name());
                            } else {
                                first = new PolicyRef(entry.name(), entry.policy());
                                ref = first;
                            }
                        } else {
                            if (!seenNames.add(entry.name().toLowerCase())) {
                                patchCallback.onError(PolicyPatchFailed.NAME_CONFLICT, entry.name());
                            } else {
                                PolicyRef newPolicy = new PolicyRef(entry.name(), entry.policy());
                                PolicyRef.addAfter(newPolicy, ref);
                                ref = newPolicy;
                            }
                        }
                    }
                    if (first != null && current.previous != null) {
                        current.previous.next = first;
                        first.previous = current.previous;
                    }
                    if (ref != null && current.next != null) {
                        ref.next = current.next;
                        current.next.previous = ref;
                    }
                    PolicyRef.remove(current);
                    current = current.next;
                    continue;
                } else if (r.action == PolicyPatchAction.REMOVE) {
                    Objects.requireNonNull(r.policyEntries);
                    seenNames.remove(current.name.toLowerCase());
                    PolicyRef.remove(current);
                    if (current == firstPolicyRef) {
                        firstPolicyRef = firstPolicyRef.next;
                    }
                    current = current.next;
                    continue;
                }
            }
            PolicyEntry[] headEntries = patchCallback.headEntries();
            if (headEntries != null) {
                int el = headEntries.length;
                PolicyRef firstHeadPolicyRef = null;
                PolicyRef ref = null;
                if (el > 0) {
                    for (int m = 0; m < el; m++) {
                        PolicyEntry entry = Objects.requireNonNull(headEntries[m]);
                        if (!seenNames.add(headEntries[m].name().toLowerCase())) {
                            patchCallback.onError(PolicyPatchFailed.NAME_CONFLICT, headEntries[m].name());
                        } else {
                            if (firstHeadPolicyRef == null) {
                                firstHeadPolicyRef = new PolicyRef(entry);
                                ref = firstHeadPolicyRef;

                            } else {
                                PolicyRef newItem = new PolicyRef(entry);
                                PolicyRef.addAfter(newItem, ref);
                                ref = newItem;
                            }
                        }
                    }

                    if (firstHeadPolicyRef != null) {
                        if (firstPolicyRef == null) {
                            firstPolicyRef = firstHeadPolicyRef;
                        } else if (ref != null) {
                            ref.next = firstPolicyRef;
                            firstPolicyRef.previous = ref;
                            firstPolicyRef = ref;
                        }
                    }
                }
            }
            //
            PolicyEntry[] tailEntries = patchCallback.tailEntries();
            if (tailEntries != null) {
                int el = tailEntries.length;
                PolicyRef firstTailPolicyRef = null;
                PolicyRef ref = null;
                if (el > 0) {
                    for (int m = 0; m < el; m++) {
                        PolicyEntry entry = Objects.requireNonNull(tailEntries[m]);
                        if (!seenNames.add(tailEntries[m].name().toLowerCase())) {
                            patchCallback.onError(PolicyPatchFailed.NAME_CONFLICT, tailEntries[m].name());
                        } else {
                            if (firstTailPolicyRef == null) {
                                firstTailPolicyRef = new PolicyRef(entry);
                                ref = firstTailPolicyRef;

                            } else {
                                PolicyRef newItem = new PolicyRef(entry);
                                PolicyRef.addAfter(newItem, ref);
                                ref = newItem;
                            }
                        }
                    }
                    if (firstTailPolicyRef != null) {
                        if (firstPolicyRef == null) {
                            firstPolicyRef = firstTailPolicyRef;
                        } else {
                            PolicyRef c = firstPolicyRef;
                            PolicyRef p = firstPolicyRef;
                            while (c != null) {
                                p = c;
                                c = c.next;
                            }
                            p.next = firstTailPolicyRef;
                            firstTailPolicyRef.previous = p;
                        }
                    }
                }
            }
        } while (patchCallback.requireRerun());
        //
        List<PolicyEntry> patchedEntries = new ArrayList<PolicyEntry>();
        PolicyRef c = firstPolicyRef;
        while (c != null) {
            patchedEntries.add(new PolicyEntry(c.name, c.policy));
            c = c.next;
        }
        //
        return new HttpPipeline(patchedEntries.toArray(new PolicyEntry[0]), this.httpClient);
    }

    /**
     * Sends the request wrapped in the provided context through pipeline.
     *
     * @param context the request context
     * @return a publisher upon subscription flows the context through policies, sends the request and emits response upon completion.
     */
    public Mono<HttpResponse> sendRequest(PipelineCallContext context) {
        return context.process();
    }
}
