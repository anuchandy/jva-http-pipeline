package com.azjvsdk.experimental.http.pipeline;

import com.azjvsdk.experimental.http.HttpResponse;
import reactor.core.publisher.Mono;

/**
 * A type that invokes next policy in the pipeline.
 */
public class NextPolicy {
    private final HttpPipeline pipeline;
    private final HttpPipelineCallContext context;
    private int currentPolicyIndex;

    /**
     * Package Private ctr.
     *
     * Creates NextPolicy.
     *
     * @param pipeline the pipeline
     * @param context the request-response context
     */
    NextPolicy(final HttpPipeline pipeline, HttpPipelineCallContext context) {
        this.pipeline = pipeline;
        this.context = context;
        this.currentPolicyIndex = -1;
    }

    /**
     * Invokes the next {@link HttpPipelinePolicy}.
     *
     * @return a publisher upon subscription invokes next policy and emits response from the policy.
     */
    public Mono<HttpResponse> process() {
        final int size = this.pipeline.pipelinePolicies().length;
        if (this.currentPolicyIndex > size) {
            return Mono.error(new IllegalStateException("There is no more policies to execute."));
        } else {
            this.currentPolicyIndex++;
            if (this.currentPolicyIndex == size) {
                return this.pipeline.httpClient().sendRequestAsync(this.context);
            } else {
                return this.pipeline.pipelinePolicies()[this.currentPolicyIndex].process(this.context, this);
            }
        }
    }
}
