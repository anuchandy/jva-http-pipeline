package com.azjvsdk.experimental.http.pipeline;

import com.azjvsdk.experimental.http.HttpResponse;
import reactor.core.publisher.Mono;

/**
 * Request policy.
 */
@FunctionalInterface
public interface RequestPolicy {
    /**
     * Process provided request context and invokes the next policy.
     *
     * @param context request context
     * @param next the next policy to invoke
     * @return publisher that initiate the request upon subscription and emits response on completion.
     */
    Mono<HttpResponse> process(PipelineCallContext context, NextPolicy next);
}
