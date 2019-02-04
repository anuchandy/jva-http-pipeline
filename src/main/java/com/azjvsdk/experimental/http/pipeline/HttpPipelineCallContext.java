package com.azjvsdk.experimental.http.pipeline;

import com.azjvsdk.experimental.http.HttpRequest;

import java.util.Objects;
import java.util.Optional;

/**
 * Type representing context local to a single http request and it's response.
 */
public final class HttpPipelineCallContext {
    private final HttpRequest httpRequest;
    private ContextData data;

    //<editor-fold defaultstate="collapsed" desc="Package internal methods">
    /**
     * Package private ctr.
     *
     * Creates HttpPipelineCallContext.
     *
     * @param httpRequest the request for which context needs to be created
     *
     * @throws IllegalArgumentException if there are multiple policies with same name
     */
    HttpPipelineCallContext(HttpRequest httpRequest) {
       this(httpRequest, ContextData.NONE);
    }

    /**
     * Package private ctr.
     *
     * Creates HttpPipelineCallContext.
     *
     * @param httpRequest the request for which context needs to be created
     * @param data the data to associate with this context
     *
     * @throws IllegalArgumentException if there are multiple policies with same name
     */
    HttpPipelineCallContext(HttpRequest httpRequest, ContextData data) {
        Objects.requireNonNull(httpRequest);
        Objects.requireNonNull(data);
        //
        this.httpRequest = httpRequest;
        this.data = data;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Public methods">

    /**
     * Stores a key-value data in the context.
     *
     * @param key the key
     * @param value the value
     */
    public void setData(String key, Object value) {
        this.data = this.data.addData(key, value);
    }

    /**
     * Gets a value with the given key stored in the context.
     *
     * @param key the key
     * @return the value
     */
    public Optional<Object> getData(String key) {
        return this.data.getData(key);
    }

    /**
     * @return the http request.
     */
    public HttpRequest httpRequest() {
        return this.httpRequest;
    }

    //</editor-fold>
}
