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
    private final HttpPipelinePolicy[] pipelinePolicies;
    private final HttpClient httpClient;

    /**
     * Creates a HttpPipeline holding array of policies that gets applied
     * to all request initiated through {@link HttpPipeline#sendRequest(HttpPipelineCallContext)}
     * and it's response.
     *
     * @param pipelinePolicies pipeline policies in the order they need to applied
     * @param httpClient the http client to write request to wire and receive response from wire.
     */
    public HttpPipeline(HttpPipelinePolicy[] pipelinePolicies, HttpClient httpClient) {
        Objects.requireNonNull(pipelinePolicies);
        Objects.requireNonNull(httpClient);
        this.pipelinePolicies = pipelinePolicies;
        this.httpClient = httpClient;
    }

    /**
     * @return policies in the pipeline.
     */
    public HttpPipelinePolicy[] pipelinePolicies() {
        return this.pipelinePolicies;
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
    public HttpPipelineCallContext newContext(HttpRequest httpRequest) {
        return new HttpPipelineCallContext(httpRequest);
    }

    /**
     * Creates a new context local to the provided http request.
     *
     * @param httpRequest the request for a context needs to be created
     * @param data the data to associate with this context
     * @return the request context
     */
    public HttpPipelineCallContext newContext(HttpRequest httpRequest, ContextData data) {
        return new HttpPipelineCallContext(httpRequest, data);
    }

    /**
     * Wraps the request in a context and send it through pipeline.
     *
     * @param request the request
     * @return a publisher upon subscription flows the context through policies, sends the request and emits response upon completion.
     */
    public Mono<HttpResponse> sendRequest(HttpRequest request) {
        return this.sendRequest(this.newContext(request));
    }

    /**
     * Sends the context through pipeline.
     *
     * @param context the request context
     * @return a publisher upon subscription flows the context through policies, sends the request and emits response upon completion.
     */
    public Mono<HttpResponse> sendRequest(HttpPipelineCallContext context) {
        NextPolicy next = new NextPolicy(this, context);
        return next.process();
    }
}
