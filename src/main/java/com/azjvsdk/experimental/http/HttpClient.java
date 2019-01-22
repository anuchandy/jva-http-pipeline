package com.azjvsdk.experimental.http;

import com.azjvsdk.experimental.http.pipeline.PipelineCallContext;
import reactor.core.publisher.Mono;

public abstract class HttpClient {
    public abstract Mono<HttpResponse> sendRequestAsync(PipelineCallContext context);
}
