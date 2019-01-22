package com.azjvsdk.experimental.http;

import java.io.Closeable;

public interface HttpClientFactory extends Closeable {
    HttpClient create(HttpClientConfiguration configuration);

    void close();
}