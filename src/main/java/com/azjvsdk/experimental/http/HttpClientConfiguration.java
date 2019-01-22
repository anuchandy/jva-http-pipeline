package com.azjvsdk.experimental.http;

import java.net.Proxy;

public class HttpClientConfiguration {
    private final Proxy proxy;

    public Proxy proxy() {
        return proxy;
    }

    public HttpClientConfiguration(Proxy proxy) {
        this.proxy = proxy;
    }
}
