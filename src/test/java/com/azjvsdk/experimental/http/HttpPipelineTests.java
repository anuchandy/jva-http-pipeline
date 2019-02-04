package com.azjvsdk.experimental.http;

import com.azjvsdk.experimental.http.pipeline.HttpPipeline;
import com.azjvsdk.experimental.http.pipeline.HttpPipelineCallContext;
import com.azjvsdk.experimental.http.pipeline.HttpPipelinePolicy;
import org.junit.Assert;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HttpPipelineTests
{
    @Test
    public void pipelineWithZeroPolicies() {
        HttpPipelinePolicy[] policies = new HttpPipelinePolicy[0];
        // Create http client
        HttpClient httpClient = createHttpClient();
        // Create pipeline with the two global policies
        HttpPipeline pipeline = new HttpPipeline(policies, httpClient);
        // Create http request
        HttpRequest httpRequest = createHttpRequest();
        // Create a context for the http request
        HttpPipelineCallContext cxt = pipeline.newContext(httpRequest);
        // Store a data in context
        cxt.setData("foo", new ArrayList<String>());
        //
        Mono<HttpResponse> responseMono = pipeline.sendRequest(cxt);
        responseMono.block();

        Optional<Object> r = cxt.getData("foo");
        if (!r.isPresent()) {
            throw new IllegalStateException("Item with key 'foo' not found.");
        }
        List<String> l = (ArrayList<String>)r.get();

        Assert.assertNotNull(l);
        Assert.assertEquals(1, l.size());
        Assert.assertEquals("httpClient", l.get(0));
    }

    @Test
    public void pipelineWithOnePolicy() {
        HttpPipelinePolicy[] policies = new HttpPipelinePolicy[1];
        // First policy
        policies[0] = (context, next) -> {
            Optional<Object> r = context.getData("foo");
            if (!r.isPresent()) {
                throw new IllegalStateException("Item with key 'foo' not found.");
            }
            ((ArrayList<String>)r.get()).add("policyA");
            return next.process();
        };
        // Create http client
        HttpClient httpClient = createHttpClient();
        // Create pipeline with the two global policies
        HttpPipeline pipeline = new HttpPipeline(policies, httpClient);
        // Create http request
        HttpRequest httpRequest = createHttpRequest();
        // Create a context for the http request
        HttpPipelineCallContext cxt = pipeline.newContext(httpRequest);
        // Store a data in context
        cxt.setData("foo", new ArrayList<String>());
        //
        Mono<HttpResponse> responseMono = pipeline.sendRequest(cxt);
        responseMono.block();
        Optional<Object> r = cxt.getData("foo");
        if (!r.isPresent()) {
            throw new IllegalStateException("Item with key 'foo' not found.");
        }
        List<String> l = (ArrayList<String>)r.get();
        Assert.assertNotNull(l);
        Assert.assertEquals(2, l.size());
        Assert.assertEquals("policyA", l.get(0));
        Assert.assertEquals("httpClient", l.get(1));
    }


    @Test
    public void pipelineWithManyPolicies() {
        HttpPipelinePolicy[] policies = new HttpPipelinePolicy[2];
        // First policy
        policies[0] = (context, next) -> {
            Optional<Object> r = context.getData("foo");
            if (!r.isPresent()) {
                throw new IllegalStateException("Item with key 'foo' not found.");
            }
            ((ArrayList<String>)r.get()).add("policyA");
            return next.process();
        };
        // Second policy
        policies[1] = (context, next) -> {
            Optional<Object> r = context.getData("foo");
            if (!r.isPresent()) {
                throw new IllegalStateException("Item with key 'foo' not found.");
            }
            ((ArrayList<String>)r.get()).add("policyB");
            return next.process();
        };
        // Create http client
        HttpClient httpClient = createHttpClient();
        // Create pipeline with the two global policies
        HttpPipeline pipeline = new HttpPipeline(policies, httpClient);
        // Create http request
        HttpRequest httpRequest = createHttpRequest();
        // Create a context for the http request
        HttpPipelineCallContext cxt = pipeline.newContext(httpRequest);
        // Store a data in context
        cxt.setData("foo", new ArrayList<String>());
        //
        Mono<HttpResponse> responseMono = pipeline.sendRequest(cxt);
        responseMono.block();
        Optional<Object> r = cxt.getData("foo");
        if (!r.isPresent()) {
            throw new IllegalStateException("Item with key 'foo' not found.");
        }
        List<String> l = (ArrayList<String>)r.get();
        Assert.assertNotNull(l);
        Assert.assertEquals(3, l.size());
        Assert.assertEquals("policyA", l.get(0));
        Assert.assertEquals("policyB", l.get(1));
        Assert.assertEquals("httpClient", l.get(2));
    }

    //
    private HttpClient createHttpClient() {
        return new HttpClient() {
            @Override
            public Mono<HttpResponse> sendRequestAsync(HttpPipelineCallContext context) {
                return Mono.defer(() -> {
                    HttpRequest request = context.httpRequest();
                    //
                    Optional<Object> r = context.getData("foo");
                    if (!r.isPresent()) {
                        throw new IllegalStateException("Item with key 'foo' not found.");
                    }
                    List<String> l = (ArrayList<String>)r.get();
                    l.add("httpClient");
                    //
                    HttpResponse response = new HttpResponse() {
                        @Override
                        public int statusCode() {
                            return 200;
                        }

                        @Override
                        public String headerValue(String headerName) {
                            return null;
                        }

                        @Override
                        public HttpHeaders headers() {
                            HttpHeaders headers = new HttpHeaders();
                            return headers;
                        }

                        @Override
                        public Flux<ByteBuffer> body() {
                            return Flux.defer(() -> Flux.just(ByteBuffer.wrap("hai".getBytes(Charset.defaultCharset()))));
                        }
                    };
                    return Mono.just(response);
                });
            }
        };
    }

    private HttpRequest createHttpRequest() {
        return new HttpRequest("", HttpMethod.GET, uri("http://contoso.com"));
    }

    private URL uri(String uri) {
        try {
            return new URL(uri);
        } catch (MalformedURLException mue) {
            throw new RuntimeException(mue);
        }
    }

}
