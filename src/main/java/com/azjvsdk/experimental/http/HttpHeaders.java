package com.azjvsdk.experimental.http;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class HttpHeaders implements Iterable<HttpHeader> {
    private final Map<String, HttpHeader> headers = new HashMap<>();

    public HttpHeaders() {
    }

    public HttpHeaders(Map<String, String> headers) {
        for (final Map.Entry<String, String> header : headers.entrySet()) {
            this.set(header.getKey(), header.getValue());
        }
    }

    public HttpHeaders(Iterable<HttpHeader> headers) {
        this();

        for (final HttpHeader header : headers) {
            this.set(header.name(), header.value());
        }
    }

    public int size() {
        return headers.size();
    }

    public HttpHeaders set(String headerName, String headerValue) {
        final String headerKey = headerName.toLowerCase();
        if (headerValue == null) {
            headers.remove(headerKey);
        }
        else {
            headers.put(headerKey, new HttpHeader(headerName, headerValue));
        }
        return this;
    }

    public String value(String headerName) {
        final HttpHeader header = getHeader(headerName);
        return header == null ? null : header.value();
    }

    public String[] values(String headerName) {
        final HttpHeader header = getHeader(headerName);
        return header == null ? null : header.values();
    }

    private HttpHeader getHeader(String headerName) {
        final String headerKey = headerName.toLowerCase();
        return headers.get(headerKey);
    }

    public Map<String, String> toMap() {
        final Map<String, String> result = new HashMap<>();
        for (final HttpHeader header : headers.values()) {
            result.put(header.name(), header.value());
        }
        return result;
    }

    @Override
    public Iterator<HttpHeader> iterator() {
        return headers.values().iterator();
    }

    @Override
    public HttpHeaders clone() {
        return new HttpHeaders(this.toMap());
    }
}
