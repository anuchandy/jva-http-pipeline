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
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

/**
 * Type representing context local to a http request and response.
 */
public final class PipelineCallContext {
    private final HttpRequest httpRequest;
    private final HttpClient httpClient;
    //
    private PolicyRef firstPolicyRef;
    private PolicyRef currentPolicyRef;
    //
    private Map<String, Object> datas = new HashMap<>();

    //<editor-fold defaultstate="collapsed" desc="Package internal methods">
    /**
     * Package private ctr.
     *
     * Creates PipelineCallContext.
     *
     * @param httpClient the http client to write the request to wire and read response from wire
     * @param httpRequest the request for which context needs to be created
     * @param requestPolicyEntries the global policies (name and policy) to be applied on the request-response
     *
     * @throws IllegalArgumentException if there are multiple policies with same name
     */
    PipelineCallContext(HttpClient httpClient, HttpRequest httpRequest, PolicyEntry[] requestPolicyEntries) {
        Objects.requireNonNull(httpClient);
        Objects.requireNonNull(httpRequest);
        Objects.requireNonNull(requestPolicyEntries);
        //
        this.httpClient = httpClient;
        this.httpRequest = httpRequest;
        //
        int size = requestPolicyEntries.length;
        if (size > 0) {
            //
            Objects.requireNonNull(requestPolicyEntries[0]);
            Set<String> seenNames = new HashSet<String>();
            seenNames.add(requestPolicyEntries[0].name().toLowerCase());
            this.firstPolicyRef = new PolicyRef(requestPolicyEntries[0]);
            //
            PolicyRef ref = this.firstPolicyRef;
            //
            for (int i = 1; i < size; i++) {
                PolicyEntry entry = Objects.requireNonNull(requestPolicyEntries[i]);
                if (!seenNames.add(entry.name().toLowerCase())) {
                    throw new IllegalArgumentException("Policy name must be unique, found multiple policies with same name '" + entry.name() + "'.");
                }
                PolicyRef newItem = new PolicyRef(entry);
                PolicyRef.addAfter(newItem, ref);
                ref = newItem;
            }
        }
        this.currentPolicyRef = null;
    }

    /**
     * Package private method.
     *
     * Gets next {@link RequestPolicy} in the pipeline.
     *
     * @return next policy in the pipeline
     */
    RequestPolicy nextPipelinePolicy() {
        this.currentPolicyRef = this.currentPolicyRef.next;
        if (this.currentPolicyRef == null) {
            return (context, next) -> httpClient.sendRequestAsync(context);
        } else {
            return this.currentPolicyRef.policy;
        }
    }

    /**
     * Package private method.
     *
     * Start processing the context which sends the request-context through pipeline.
     *
     * @return a publisher upon subscription flows the context through policies, sends the request and emits response upon completion.
     */
    Mono<HttpResponse> process() {
        // Use defer to ensure policy execution happens only after subscription.
        return Mono.defer(() -> {
            if (this.firstPolicyRef == null) {
                return this.httpClient.sendRequestAsync(this);
            } else {
                this.currentPolicyRef = this.firstPolicyRef;
                return this.firstPolicyRef.policy.process(this, new NextPolicy(this));
            }
        });
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Public methods">

    /**
     * @return global and local request policy entries.
     */
    public PolicyEntry[] requestPolicyEntries() {
        List<PolicyEntry> policies = new ArrayList<PolicyEntry>();
        PolicyRef current = this.firstPolicyRef;
        while (current != null) {
            policies.add(new PolicyEntry(current.name, current.policy));
            current = current.next;
        }
        return policies.toArray(new PolicyEntry[0]);
    }

    /**
     * Stores a key-value data in the context.
     *
     * @param key the key
     * @param value the value
     */
    public void setData(String key, Object value) {
        this.datas.put(key, value);
    }

    /**
     * Gets a value with the given key stored in the context.
     *
     * @param key the key
     * @return the value if exists else null
     */
    public Object getData(String key) {
        return this.datas.get(key);
    }

    /**
     * Checks data with given key exists in the context.
     *
     * @param key the key
     * @return true if key exists, false otherwise.
     */
    public boolean dataExists(String key) {
        return datas.containsKey(key);
    }

    /**
     * @return the http request.
     */
    public HttpRequest httpRequest() {
        return this.httpRequest;
    }

    /**
     * Inserts a local policy at beginning of request policies associated with this context.
     *
     * @param name the policy name
     * @param policy the policy
     *
     * @throws NullPointerException
     *         if the specified {@code name} or {@code policy} is {@code null}
     * @throws IllegalArgumentException
     *         if there is already a policy with the same {@code name}
     * @throws IllegalStateException
     *         if the specified {@code policy} cannot be added because request flow already started
     */
    public void addPolicyFirst(String name, RequestPolicy policy) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(policy);
        if (pipelineFlowStarted()) {
            throw new IllegalStateException("Cannot add policy in the beginning, request already went through existing first policy.");
        } else {
            PolicyRef newPolicyRef = new PolicyRef(name, policy);
            if (this.firstPolicyRef != null) {
                PolicyRef.addBefore(newPolicyRef, this.firstPolicyRef);
            }
            this.firstPolicyRef = newPolicyRef;
        }
    }

    /**
     * Inserts a local policy at the end of request policies associated with this context.
     *
     * @param name the policy name
     * @param policy the policy
     *
     * @throws NullPointerException
     *         if the specified {@code name} or {@code policy} is {@code null}
     * @throws IllegalArgumentException
     *         if there is already a policy with the same {@code name}
     */
    public void addPolicyLast(String name, RequestPolicy policy) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(policy);
        //
        PolicyRef current = this.firstPolicyRef;
        PolicyRef prev = null;
        while (current != null) {
            if (current.name.equalsIgnoreCase(name)) {
                throw new IllegalArgumentException("A policy with the name '" + name + "' already exists.");
            }
            prev = current;
            current = current.next;
        }
        if (prev == null) {
            this.firstPolicyRef = new PolicyRef(name, policy);
        } else {
            PolicyRef.addAfter(new PolicyRef(name, policy), prev);
        }
    }

    /**
     * Inserts a local policy after an existing policy in the request policies associated with this context.
     *
     * @param baseName  the name of the existing policy
     * @param name      the name of the new policy to insert after existing policy
     * @param policy    the policy
     *
     * @throws NullPointerException
     *         if the specified {@code baseName}, {@code name} or {@code policy} is {@code null}
     * @throws IllegalArgumentException
     *         if there is already a policy with the same {@code name}
     * @throws NoSuchElementException
     *         if there's no such policy with the specified {@code baseName}
     * @throws IllegalStateException
     *         if {@code policy} cannot be inserted because request already went through base policy and it's next policy
     */
    public void addPolicyAfter(String baseName, String name, RequestPolicy policy) {
        Objects.requireNonNull(baseName);
        Objects.requireNonNull(name);
        Objects.requireNonNull(policy);
        //
        LookUpResult result = this.lookupPolicyAndValidate(baseName, name);
        if (!pipelineFlowStarted()) {
            PolicyRef.addAfter(new PolicyRef(name, policy), result.basePolicy);
        } else {
            if (result.currentIsBeforeBasePolicy) {
                PolicyRef.addAfter(new PolicyRef(name, policy), result.basePolicy);
            } else {
                throw new IllegalStateException("Cannot add the policy '" + name + "' after '" + baseName + "', the request already went through the policy '" + baseName + "' and it's next policy.");
            }
        }
    }

    /**
     * Inserts a local policy before an existing policy in the request policies associated with this context.
     *
     * @param baseName the name of the existing policy
     * @param name the name of the new policy to insert before existing policy
     * @param policy the policy
     *
     * @throws NullPointerException
     *         if the specified {@code baseName}, {@code name} or {@code policy} is {@code null}
     * @throws IllegalArgumentException
     *         if there is already a policy with the same {@code name}
     * @throws NoSuchElementException
     *         if there's no such policy with the specified {@code baseName}
     * @throws IllegalStateException
     *         if {@code policy} cannot be inserted because request already went through base policy
     */
    public void addPolicyBefore(String baseName, String name, RequestPolicy policy) {
        Objects.requireNonNull(baseName);
        Objects.requireNonNull(name);
        Objects.requireNonNull(policy);
        //
        LookUpResult result = this.lookupPolicyAndValidate(baseName, name);
        if (!pipelineFlowStarted()) {
            PolicyRef newPolicyRef = new PolicyRef(name, policy);
            PolicyRef.addBefore(newPolicyRef, result.basePolicy);
            if (result.basePolicy == this.firstPolicyRef) {
                this.firstPolicyRef = newPolicyRef;
            }
        } else {
            if (result.currentIsBeforeBasePolicy && result.basePolicy != this.currentPolicyRef) {
                PolicyRef.addBefore(new PolicyRef(name, policy), result.basePolicy);
            } else {
                throw new IllegalStateException("Cannot add the policy '" + name + "' before '" + baseName + "', the request already went through the policy '" + baseName + "'.");
            }
        }
    }

    /**
     * Replaces a policy in the request policies associated with this context with new local policy.
     *
     * @param baseName the name of the existing policy to be replaced
     * @param name the name of the new policy
     * @param policy the policy
     *
     * @throws NullPointerException
     *         if the specified {@code baseName}, {@code name} or {@code policy} is {@code null}
     * @throws IllegalArgumentException
     *         if there is already a policy with the same {@code name} and is not policy with name {@code baseName}
     * @throws NoSuchElementException
     *         if there's no such policy with the specified {@code baseName}
     * @throws IllegalStateException
     *         if {@code policy} cannot be inserted because request already went through base policy
     */
    public void replacePolicy(String baseName, String name, RequestPolicy policy) {
        Objects.requireNonNull(baseName);
        Objects.requireNonNull(name);
        Objects.requireNonNull(policy);
        //
        LookUpResult result = this.lookupPolicy(baseName, name);
        if (result.basePolicy == null) {
            throw new NoSuchElementException("There is no policy with the name '" + baseName + "'.");
        }
        if (result.nameExists && !baseName.equalsIgnoreCase(name)) {
            throw new IllegalArgumentException("A policy with the name '" + name + "' already exists.");
        }
        if (!pipelineFlowStarted()) {
            PolicyRef newPolicyRef = new PolicyRef(name, policy);
            PolicyRef.replace(newPolicyRef, result.basePolicy);
            if (result.basePolicy == this.firstPolicyRef) {
                this.firstPolicyRef = newPolicyRef;
            }
        } else {
            if (result.currentIsBeforeBasePolicy && result.basePolicy != this.currentPolicyRef) {
                PolicyRef.replace(new PolicyRef(name, policy), result.basePolicy);
            } else {
                throw new IllegalStateException("Cannot replace the policy '" + baseName + ", the request already went through the policy '" + baseName + "'.");
            }
        }
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Private methods">

    /**
     * @return true if the request started flowing through the pipeline.
     */
    private boolean pipelineFlowStarted() {
        return this.currentPolicyRef != null;
    }

    /**
     * Helper method to perform following:
     *
     * 1. locate a policy with the given {@code baseName} if exists
     * 2. locate if there is an existing policy with given {@code name}
     * 3. checks the currently active policy is before the policy with name {@code baseName}
     *
     * @param baseName name of the policy to locate
     * @param name name of the policy to see if it exists
     * @return the loop up result.
     *
     * @throws NoSuchElementException if a policy with name {@code baseName} not exists
     * @throws IllegalArgumentException if a policy with name {@code name} exists
     */
    private LookUpResult lookupPolicyAndValidate(String baseName, String name) {
        LookUpResult result = this.lookupPolicy(baseName, name);
        if (result.nameExists) {
            throw new IllegalArgumentException("A policy with the name '" + name + "' already exists.");
        }
        if (result.basePolicy == null) {
            throw new NoSuchElementException("There is no policy with the name '" + baseName + "'.");
        }
        return result;
    }

    /**
     * Helper method to perform following:
     *
     * 1. locate a policy with the given {@code baseName} if exists
     * 2. locate if there is an existing policy with given {@code name}
     * 3. checks the currently active policy is before the policy with name {@code baseName}
     *
     * @param baseName name of the policy to locate
     * @param name name of the policy to see if it exists
     * @return the lookup result.
     */
    private LookUpResult lookupPolicy(String baseName, String name) {
        PolicyRef basePolicy = null;
        boolean nameExists = false;
        boolean currentIsBeforeBasePolicy = false;
        //
        PolicyRef ref = this.firstPolicyRef;
        while (ref != null) {
            if (basePolicy != null && nameExists) {
                break;
            }
            if (basePolicy == null) {
                if (this.currentPolicyRef == ref) {
                    currentIsBeforeBasePolicy = true;
                }
                if (ref.name.equalsIgnoreCase(baseName)) {
                    basePolicy = ref;
                }
            }
            if (!nameExists && ref.name.equalsIgnoreCase(name)) {
                nameExists = true;
            }
            ref = ref.next;
        }
        return new LookUpResult(basePolicy, nameExists, currentIsBeforeBasePolicy);
    }

    /**
     * Type representing result of {@link this#lookupPolicy(String, String)}
     * and {@link this#lookupPolicyAndValidate(String, String)}.
     */
    private static class LookUpResult {
        final PolicyRef basePolicy;
        final boolean nameExists;
        final boolean currentIsBeforeBasePolicy;

        private LookUpResult(PolicyRef basePolicy, boolean nameExists, boolean currentIsBeforeBasePolicy) {
            this.basePolicy = basePolicy;
            this.nameExists = nameExists;
            this.currentIsBeforeBasePolicy = currentIsBeforeBasePolicy;
        }
    }
    //</editor-fold>
}
