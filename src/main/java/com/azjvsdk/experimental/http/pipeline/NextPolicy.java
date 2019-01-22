package com.azjvsdk.experimental.http.pipeline;

import com.azjvsdk.experimental.http.HttpResponse;
import reactor.core.publisher.Mono;

/**
 * A type that invokes next policy in the pipeline.
 */
public class NextPolicy {
    private final PipelineCallContext context;

    /**
     * Package Private ctr.
     *
     * Creates NextPolicy.
     *
     * @param context the context to pass to the next {@link RequestPolicy}
     */
    NextPolicy(final PipelineCallContext context) {
        this.context = context;
    }

    /**
     * Invokes the next {@link RequestPolicy}.
     *
     * @return a publisher upon subscription invokes next policy and emits response from the policy.
     */
    public Mono<HttpResponse> process() {
        return this.context.nextPipelinePolicy().process(this.context, this);
    }
}
