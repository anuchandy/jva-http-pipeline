package com.azjvsdk.experimental.http;

import com.azjvsdk.experimental.http.pipeline.HttpPipelineCallContext;
import reactor.core.publisher.Mono;

public abstract class HttpClient {
    public abstract Mono<HttpResponse> sendRequestAsync(HttpPipelineCallContext context);
}
