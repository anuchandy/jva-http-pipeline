package com.azjvsdk.experimental.http.pipeline;

import com.azjvsdk.experimental.http.HttpRequest;
import com.azjvsdk.experimental.http.HttpResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.time.Duration;

public abstract class HttpRetryPolicy implements HttpPipelinePolicy {
    @Override
    public Mono<HttpResponse> process(HttpPipelineCallContext context, NextPolicy next) {
        return attemptAsync(context, next, 0);
    }

    private Mono<HttpResponse> attemptAsync(final HttpPipelineCallContext context, NextPolicy next, final int tryCount) {
        //
        HttpRequest httpRequest = new HttpRequest(context.httpRequest().httpMethod(), context.httpRequest().url());
        httpRequest.withHeaders(context.httpRequest().headers().clone());
        if (!context.isRequestContentReplyable()) {
            List<ByteB>
            context.setRequestContentReplayable(true);
        } else {
            httpRequest.withBody(context.httpRequest().body());
        }
        //
        Flux<ByteBuffer> fg = null;
        fg.buffer()


        return next.sendAsync(httpRequest.buffer())
                .flatMap(httpResponse -> {
                    if (shouldRetry(httpResponse, tryCount)) {
                        return attemptAsync(httpRequest, tryCount + 1).delaySubscription(Duration.of(delayTime, timeUnit));
                    } else {
                        return Mono.just(httpResponse);
                    }
                })
                .onErrorResume(err -> {
                    if (tryCount < maxRetries) {
                        return attemptAsync(httpRequest, tryCount + 1).delaySubscription(Duration.of(delayTime, timeUnit));
                    } else {
                        return Mono.error(err);
                    }
                });
    }

    protected abstract bool shouldRetry(HttpResponse response, int attempted, out TimeSpan delay);
}
