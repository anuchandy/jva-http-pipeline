package com.azjvsdk.experimental.http;

import reactor.core.publisher.Flux;

import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class HttpRequest {
    private String callerMethod;
    private HttpMethod httpMethod;
    private URL url;
    private HttpHeaders headers;
    private Flux<ByteBuffer> body;

    public HttpRequest(String callerMethod, HttpMethod httpMethod, URL url) {
        this.callerMethod = callerMethod;
        this.httpMethod = httpMethod;
        this.url = url;
        this.headers = new HttpHeaders();
    }

    public HttpRequest(HttpMethod httpMethod, URL url, HttpHeaders headers, Flux<ByteBuffer> body) {
        this.httpMethod = httpMethod;
        this.url = url;
        this.headers = headers;
        this.body = body;
    }

    public HttpMethod httpMethod() {
        return httpMethod;
    }

    public HttpRequest withHttpMethod(HttpMethod httpMethod) {
        this.httpMethod = httpMethod;
        return this;
    }

    public URL url() {
        return url;
    }

    public HttpRequest withUrl(URL url) {
        this.url = url;
        return this;
    }

    public HttpHeaders headers() {
        return headers;
    }

    public HttpRequest withHeaders(HttpHeaders headers) {
        this.headers = headers;
        return this;
    }

    public HttpRequest withHeader(String headerName, String headerValue) {
        headers.set(headerName, headerValue);
        return this;
    }

    public Flux<ByteBuffer> body() {
        return body;
    }

    public HttpRequest withBody(String body) {
        final byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        return withBody(bodyBytes);
    }

    public HttpRequest withBody(byte[] body) {
        headers.set("Content-Length", String.valueOf(body.length));
        return withBody(Flux.just(ByteBuffer.wrap(body)));
    }

    public HttpRequest withBody(Flux<ByteBuffer> body) {
        this.body = body;
        return this;
    }
}
