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
    private final RequestPolicy[] requestPolicies;
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
        this.requestPolicies = requestPolicies;
        this.httpClient = httpClient;
    }

    /**
     * @return global request policies in the pipeline.
     */
    public RequestPolicy[] requestPolicies() {
        return this.requestPolicies;
    }

    /**
     * @return the http client associated with the pipeline.
     */
    public HttpClient httpClient() {
        return this.httpClient;
    }

    /**
     * Creates a new context local to the provided http request.
     *
     * @param httpRequest the request for a context needs to be created
     * @return the request context
     */
    public PipelineCallContext newContext(HttpRequest httpRequest) {
        return new PipelineCallContext(httpRequest, this);
    }

    /**
     * Creates a new context local to the provided http request.
     *
     * @param httpRequest the request for a context needs to be created
     * @param data the data to associate with this context
     * @return the request context
     */
    public PipelineCallContext newContext(HttpRequest httpRequest, ContextData data) {
        return new PipelineCallContext(httpRequest, this, data);
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
