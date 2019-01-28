package com.azjvsdk.experimental.http;

import com.azjvsdk.experimental.http.pipeline.HttpPipeline;
import com.azjvsdk.experimental.http.pipeline.PipelineCallContext;
import com.azjvsdk.experimental.http.pipeline.RequestPolicy;
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

public class HttpPipelineTests
{
    @Test
    public void pipelineWithZeroPolicies() {
        RequestPolicy[] policies = new RequestPolicy[0];
        // Create http client
        HttpClient httpClient = createHttpClient();
        // Create pipeline with the two global policies
        HttpPipeline pipeline = new HttpPipeline(policies, httpClient);
        // Create http request
        HttpRequest httpRequest = createHttpRequest();
        // Create a context for the http request
        PipelineCallContext cxt = pipeline.newContext(httpRequest);
        // Store a data in context
        cxt.setData("foo", new ArrayList<String>());
        //
        Mono<HttpResponse> responseMono = pipeline.sendRequest(cxt);
        responseMono.block();
        List<String> l = (ArrayList<String>) cxt.getData("foo");
        Assert.assertNotNull(l);
        Assert.assertEquals(1, l.size());
        Assert.assertEquals("httpClient", l.get(0));
    }

    @Test
    public void pipelineWithOnePolicy() {
        RequestPolicy[] policies = new RequestPolicy[1];
        // First policy
        policies[0] = (context, next) -> {
            List<String> l = (ArrayList<String>) context.getData("foo");
            l.add("policyA");
            return next.process();
        };
        // Create http client
        HttpClient httpClient = createHttpClient();
        // Create pipeline with the two global policies
        HttpPipeline pipeline = new HttpPipeline(policies, httpClient);
        // Create http request
        HttpRequest httpRequest = createHttpRequest();
        // Create a context for the http request
        PipelineCallContext cxt = pipeline.newContext(httpRequest);
        // Store a data in context
        cxt.setData("foo", new ArrayList<String>());
        //
        Mono<HttpResponse> responseMono = pipeline.sendRequest(cxt);
        responseMono.block();
        List<String> l = (ArrayList<String>) cxt.getData("foo");
        Assert.assertNotNull(l);
        Assert.assertEquals(2, l.size());
        Assert.assertEquals("policyA", l.get(0));
        Assert.assertEquals("httpClient", l.get(1));
    }


    @Test
    public void pipelineWithManyPolicies() {
        RequestPolicy[] policies = new RequestPolicy[2];
        // First policy
        policies[0] = (context, next) -> {
            List<String> l = (ArrayList<String>) context.getData("foo");
            l.add("policyA");
            return next.process();
        };
        // Second policy
        policies[1] = (context, next) -> {
            List<String> l = (ArrayList<String>) context.getData("foo");
            l.add("policyB");
            return next.process();
        };
        // Create http client
        HttpClient httpClient = createHttpClient();
        // Create pipeline with the two global policies
        HttpPipeline pipeline = new HttpPipeline(policies, httpClient);
        // Create http request
        HttpRequest httpRequest = createHttpRequest();
        // Create a context for the http request
        PipelineCallContext cxt = pipeline.newContext(httpRequest);
        // Store a data in context
        cxt.setData("foo", new ArrayList<String>());
        //
        Mono<HttpResponse> responseMono = pipeline.sendRequest(cxt);
        responseMono.block();
        List<String> l = (ArrayList<String>) cxt.getData("foo");
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
            public Mono<HttpResponse> sendRequestAsync(PipelineCallContext context) {
                return Mono.defer(() -> {
                    HttpRequest request = context.httpRequest();
                    //
                    List<String> l = (ArrayList<String>) context.getData("foo");
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
