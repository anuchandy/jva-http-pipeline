package com.azjvsdk.experimental.http.pipeline;

import com.azjvsdk.experimental.http.HttpRequest;
import com.azjvsdk.experimental.http.HttpResponse;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Type representing context local to a http request and response.
 */
public final class PipelineCallContext {
    private final HttpPipeline httpPipeline;
    private final HttpRequest httpRequest;
    private int currentPolicyIndex;
    //
    private Map<String, Object> datas = new HashMap<>();

    //<editor-fold defaultstate="collapsed" desc="Package internal methods">
    /**
     * Package private ctr.
     *
     * Creates PipelineCallContext.
     *
     * @param httpRequest the request for which context needs to be created
     * @param httpPipeline the http pipeline
     *
     * @throws IllegalArgumentException if there are multiple policies with same name
     */
    PipelineCallContext(HttpRequest httpRequest, HttpPipeline httpPipeline) {
        Objects.requireNonNull(httpRequest);
        Objects.requireNonNull(httpPipeline);
        //
        this.httpRequest = httpRequest;
        this.httpPipeline = httpPipeline;
        this.currentPolicyIndex = this.httpPipeline.requestPolicies().length == 0 ? -1 : 0;
    }

    /**
     * Package private method.
     *
     * Gets next {@link RequestPolicy} in the pipeline.
     *
     * @return next policy in the pipeline
     */
    RequestPolicy nextPipelinePolicy() {
        this.currentPolicyIndex++;
        if (this.currentPolicyIndex == this.httpPipeline.requestPolicies().length) {
            return (context, next) -> this.httpPipeline.httpClient().sendRequestAsync(context);
        } else {
            return this.httpPipeline.requestPolicies()[this.currentPolicyIndex];
        }
    }

    /**
     * Package private method.
     *
     * Start processing the context which sends the request-context through pipeline.
     *
     * @return a publisher upon subscription flows the context through policies, sends the request and emits response upon completion.
     */
    Mono<HttpResponse> process() {
        // Use defer to ensure policy execution happens only after subscription.
        return Mono.defer(() -> {
            if (this.currentPolicyIndex == -1) {
                return this.httpPipeline.httpClient().sendRequestAsync(this);
            } else {
                return this.httpPipeline.requestPolicies()[0].process(this, new NextPolicy(this));
            }
        });
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
        this.datas.put(key, value);
    }

    /**
     * Gets a value with the given key stored in the context.
     *
     * @param key the key
     * @return the value if exists else null
     */
    public Object getData(String key) {
        return this.datas.get(key);
    }

    /**
     * Checks data with given key exists in the context.
     *
     * @param key the key
     * @return true if key exists, false otherwise.
     */
    public boolean dataExists(String key) {
        return datas.containsKey(key);
    }

    /**
     * @return the http request.
     */
    public HttpRequest httpRequest() {
        return this.httpRequest;
    }

    //</editor-fold>
}
