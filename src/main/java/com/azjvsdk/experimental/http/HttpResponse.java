package com.azjvsdk.experimental.http;

import reactor.core.publisher.Flux;

import java.io.Closeable;
import java.nio.ByteBuffer;

public abstract class HttpResponse implements Closeable {
    private HttpRequest request;

    public abstract int statusCode();

    public abstract String headerValue(String headerName);

    public abstract HttpHeaders headers();

    public abstract Flux<ByteBuffer> body();

    public final HttpRequest request() {
        return request;
    }

    public void close() {
        // no-op
    }

    public final HttpResponse withRequest(HttpRequest request) {
        this.request = request;
        return this;
    }
}
