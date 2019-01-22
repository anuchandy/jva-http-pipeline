package com.azjvsdk.experimental.http.pipeline;

import com.azjvsdk.experimental.http.HttpClient;
import com.azjvsdk.experimental.http.HttpRequest;
import com.azjvsdk.experimental.http.HttpResponse;
import reactor.core.publisher.Mono;

import java.util.Objects;

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
     * Sends the request wrapped in the provided context through pipeline.
     *
     * @param context the request context
     * @return a publisher upon subscription flows the context through policies, sends the request and emits response upon completion.
     */
    public Mono<HttpResponse> sendRequest(PipelineCallContext context) {
        return context.process();
    }
}
