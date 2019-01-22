package com.azjvsdk.experimental.http;

import com.azjvsdk.experimental.http.pipeline.HttpPipeline;
import com.azjvsdk.experimental.http.pipeline.PipelineCallContext;
import com.azjvsdk.experimental.http.pipeline.PolicyEntry;
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
    public void pipelineWithGlobalPolicies() {
        RequestPolicy[] globalPolicies = new RequestPolicy[2];
        //
        // First global policy
        globalPolicies[0] = (context, next) -> {
            List<String> l = (ArrayList<String>) context.getData("foo");
            l.add("gPolicyA");
            return next.process();
        };
        // Second global policy
        globalPolicies[1] = (context, next) -> {
            List<String> l = (ArrayList<String>) context.getData("foo");
            l.add("gPolicyB");
            return next.process();
        };
        // Create http client
        HttpClient httpClient = createHttpClient();
        // Create pipeline with the two global policies
        HttpPipeline pipeline = new HttpPipeline(globalPolicies, httpClient);
        // Create http request
        HttpRequest httpRequest = createHttpRequest();
        // Create a context for the http request
        PipelineCallContext cxt = pipeline.newContext(httpRequest);
        // Store a data in context
        cxt.setData("foo", new ArrayList<String>());
        //
        Mono<HttpResponse> responseMono = pipeline.sendRequest(cxt);
        responseMono.block();
        //
        List<String> l = (ArrayList<String>) cxt.getData("foo");
        Assert.assertNotNull(l);
        Assert.assertEquals(3, l.size());
        Assert.assertEquals("gPolicyA", l.get(0));
        Assert.assertEquals("gPolicyB", l.get(1));
        Assert.assertEquals("httpClient", l.get(2));
    }

    @Test
    public void pipelineWithGlobalAndStaticLocalPolicies() {
        RequestPolicy[] globalPolicies = new RequestPolicy[2];
        //
        // First global policy
        globalPolicies[0] = (context, next) -> {
            List<String> l = (ArrayList<String>) context.getData("foo");
            l.add("gPolicyA");
            return next.process();
        };
        // Second global policy
        globalPolicies[1] = (context, next) -> {
            List<String> l = (ArrayList<String>) context.getData("foo");
            l.add("gPolicyB");
            return next.process();
        };
        // Create http client
        HttpClient httpClient = createHttpClient();
        // Create pipeline with the two global policies
        HttpPipeline pipeline = new HttpPipeline(globalPolicies, httpClient);
        // Create http request
        HttpRequest httpRequest = createHttpRequest();
        // Create a context for the first http request
        PipelineCallContext cxt = pipeline.newContext(httpRequest);
        // Add local policy 1
        cxt.addPolicyLast("lPolicyA", (context, next) -> {
                List<String> l = (ArrayList<String>) context.getData("foo");
                l.add("lPolicyA");
                return next.process();
        });
        cxt.addPolicyLast("lPolicyB", (context, next) -> {
            List<String> l = (ArrayList<String>) context.getData("foo");
            l.add("lPolicyB");
            return next.process();
        });
        // Store a data in context
        cxt.setData("foo", new ArrayList<String>());
        //
        Mono<HttpResponse> responseMono = pipeline.sendRequest(cxt);
        responseMono.block();
        //

        List<String> l = (ArrayList<String>) cxt.getData("foo");
        Assert.assertNotNull(l);
        Assert.assertEquals(5, l.size());
        Assert.assertEquals("gPolicyA", l.get(0));
        Assert.assertEquals("gPolicyB", l.get(1));
        Assert.assertEquals("lPolicyA", l.get(2));
        Assert.assertEquals("lPolicyB", l.get(3));
        Assert.assertEquals("httpClient", l.get(4));

        // ------> [Ensure global policies are not changed]

        // Create a context for the second http request
        cxt = pipeline.newContext(httpRequest);
        // Store a data in context
        cxt.setData("foo", new ArrayList<String>());
        //
        responseMono = pipeline.sendRequest(cxt);
        responseMono.block();
        //
        l = (ArrayList<String>) cxt.getData("foo");
        Assert.assertNotNull(l);
        Assert.assertEquals(3, l.size());
        Assert.assertEquals("gPolicyA", l.get(0));
        Assert.assertEquals("gPolicyB", l.get(1));
        Assert.assertEquals("httpClient", l.get(2));
    }

    @Test
    public void pipelineWithNamedGlobalAndStaticLocalPolicies() {
        PolicyEntry[] globalPolicyEntries = new PolicyEntry[2];
        //
        // First named global policy
        globalPolicyEntries[0] = new PolicyEntry("gPolicyA", (context, next) -> {
            List<String> l = (ArrayList<String>) context.getData("foo");
            l.add("gPolicyA");
            return next.process();
        });
        // Second named global policy
        globalPolicyEntries[1] = new PolicyEntry("gPolicyB", (context, next) -> {
            List<String> l = (ArrayList<String>) context.getData("foo");
            l.add("gPolicyB");
            return next.process();
        });
        // Create http client
        HttpClient httpClient = createHttpClient();
        // Create pipeline with the two global policies
        HttpPipeline pipeline = new HttpPipeline(globalPolicyEntries, httpClient);
        // Create http request
        HttpRequest httpRequest = createHttpRequest();
        // Create a context for the first http request
        PipelineCallContext cxt = pipeline.newContext(httpRequest);
        // Add local policy 1 after 1st global policy
        cxt.addPolicyAfter("gPolicyA", "lPolicyA", (context, next) -> {
            List<String> l = (ArrayList<String>) context.getData("foo");
            l.add("lPolicyA");
            return next.process();
        });
        // Add local policy 2 after second global policy
        cxt.addPolicyAfter("gPolicyB", "lPolicyB", (context, next) -> {
            List<String> l = (ArrayList<String>) context.getData("foo");
            l.add("lPolicyB");
            return next.process();
        });
        // Store a data in context
        cxt.setData("foo", new ArrayList<String>());
        //
        Mono<HttpResponse> responseMono = pipeline.sendRequest(cxt);
        responseMono.block();
        //
        List<String> l = (ArrayList<String>) cxt.getData("foo");
        Assert.assertNotNull(l);
        Assert.assertEquals(5, l.size());
        Assert.assertEquals("gPolicyA", l.get(0));
        Assert.assertEquals("lPolicyA", l.get(1));
        Assert.assertEquals("gPolicyB", l.get(2));
        Assert.assertEquals("lPolicyB", l.get(3));
        Assert.assertEquals("httpClient", l.get(4));

        // ------> [Ensure global policies are not changed]

        // Create a context for the second http request
        cxt = pipeline.newContext(httpRequest);
        // Store a data in context
        cxt.setData("foo", new ArrayList<String>());
        //
        responseMono = pipeline.sendRequest(cxt);
        responseMono.block();
        //
        l = (ArrayList<String>) cxt.getData("foo");
        Assert.assertNotNull(l);
        Assert.assertEquals(3, l.size());
        Assert.assertEquals("gPolicyA", l.get(0));
        Assert.assertEquals("gPolicyB", l.get(1));
        Assert.assertEquals("httpClient", l.get(2));
    }

    @Test
    public void pipelineGlobalPolicyReplacedWithStaticLocalPolicy() {
        PolicyEntry[] globalPolicyEntries = new PolicyEntry[2];
        //
        // First named global policy
        globalPolicyEntries[0] = new PolicyEntry("gPolicyA", (context, next) -> {
            List<String> l = (ArrayList<String>) context.getData("foo");
            l.add("gPolicyA");
            return next.process();
        });
        // Second named global policy
        globalPolicyEntries[1] = new PolicyEntry("gPolicyB", (context, next) -> {
            List<String> l = (ArrayList<String>) context.getData("foo");
            l.add("gPolicyB");
            return next.process();
        });
        // Create http client
        HttpClient httpClient = createHttpClient();
        // Create pipeline with the two global policies
        HttpPipeline pipeline = new HttpPipeline(globalPolicyEntries, httpClient);
        // Create http request
        HttpRequest httpRequest = createHttpRequest();
        // Create a context for the first http request
        PipelineCallContext cxt = pipeline.newContext(httpRequest);
        // Replace global policy with local policy
        cxt.replacePolicy("gPolicyA", "lPolicyA", (context, next) -> {
            List<String> l = (ArrayList<String>) context.getData("foo");
            l.add("lPolicyA");
            return next.process();
        });
        //
        // Store a data in context
        cxt.setData("foo", new ArrayList<String>());
        //
        Mono<HttpResponse> responseMono = pipeline.sendRequest(cxt);
        responseMono.block();
        //
        List<String> l = (ArrayList<String>) cxt.getData("foo");
        Assert.assertNotNull(l);
        Assert.assertEquals(3, l.size());
        Assert.assertEquals("lPolicyA", l.get(0));
        Assert.assertEquals("gPolicyB", l.get(1));
        Assert.assertEquals("httpClient", l.get(2));

        // ------> [Ensure global policies are not changed]

        // Create a context for the second http request
        cxt = pipeline.newContext(httpRequest);
        // Store a data in context
        cxt.setData("foo", new ArrayList<String>());
        //
        responseMono = pipeline.sendRequest(cxt);
        responseMono.block();
        //
        l = (ArrayList<String>) cxt.getData("foo");
        Assert.assertNotNull(l);
        Assert.assertEquals(3, l.size());
        Assert.assertEquals("gPolicyA", l.get(0));
        Assert.assertEquals("gPolicyB", l.get(1));
        Assert.assertEquals("httpClient", l.get(2));
    }

    @Test
    public void pipelineWithOnlyStaticLocalPolicy() {
        // Create http client
        HttpClient httpClient = createHttpClient();
        // Create pipeline with the two global policies
        HttpPipeline pipeline = new HttpPipeline(new RequestPolicy[0], httpClient);
        // Create http request
        HttpRequest httpRequest = createHttpRequest();
        // Create a context for the first http request
        PipelineCallContext cxt = pipeline.newContext(httpRequest);
        // Add a local policy in the beginning
        cxt.addPolicyFirst("lPolicyA", (context, next) -> {
            List<String> l = (ArrayList<String>) context.getData("foo");
            l.add("lPolicyA");
            return next.process();
        });
        // Add local policy 2 after first local policy
        cxt.addPolicyAfter("lPolicyA", "lPolicyB", (context, next) -> {
            List<String> l = (ArrayList<String>) context.getData("foo");
            l.add("lPolicyB");
            return next.process();
        });
        // Store a data in context
        cxt.setData("foo", new ArrayList<String>());
        //
        Mono<HttpResponse> responseMono = pipeline.sendRequest(cxt);
        responseMono.block();
        //
        List<String> l = (ArrayList<String>) cxt.getData("foo");
        Assert.assertNotNull(l);
        Assert.assertEquals(3, l.size());
        Assert.assertEquals("lPolicyA", l.get(0));
        Assert.assertEquals("lPolicyB", l.get(1));
        Assert.assertEquals("httpClient", l.get(2));

        // ------> [Ensure global policies are not changed]

        // Create a context for the second http request
        cxt = pipeline.newContext(httpRequest);
        // Store a data in context
        cxt.setData("foo", new ArrayList<String>());
        //
        responseMono = pipeline.sendRequest(cxt);
        responseMono.block();
        //
        l = (ArrayList<String>) cxt.getData("foo");
        Assert.assertNotNull(l);
        Assert.assertEquals(1, l.size());
        Assert.assertEquals("httpClient", l.get(0));
    }

    @Test
    public void pipelineWithGlobalAndDynamicLocalPolicy() {
        PolicyEntry[] globalPolicyEntries = new PolicyEntry[2];
        //
        // First named global policy
        globalPolicyEntries[0] = new PolicyEntry("gPolicyA", (context, next) -> {
            List<String> l = (ArrayList<String>) context.getData("foo");
            l.add("gPolicyA");
            return next.process();
        });
        // Second named global policy
        globalPolicyEntries[1] = new PolicyEntry("gPolicyB", (context, next) -> {
            List<String> l = (ArrayList<String>) context.getData("foo");
            l.add("gPolicyB");
            return next.process();
        });
        // Create http client
        HttpClient httpClient = createHttpClient();
        // Create pipeline with the two global policies
        HttpPipeline pipeline = new HttpPipeline(globalPolicyEntries, httpClient);
        // Create http request
        HttpRequest httpRequest = createHttpRequest();
        // Create a context for the first http request
        PipelineCallContext cxt = pipeline.newContext(httpRequest);
        // Add a local policy in the beginning that dynamically change the pipeline
        cxt.addPolicyFirst("lPolicyA", (context, next) -> {
            List<String> l1 = (ArrayList<String>) context.getData("foo");
            l1.add("lPolicyA");
            //
            context.replacePolicy("gPolicyB", "lPolicyB", (c, n) -> {
                List<String> l2 = (ArrayList<String>) context.getData("foo");
                l2.add("lPolicyB");
                return n.process();
            });
            context.addPolicyBefore("lPolicyB", "lPolicyC", (c, n) -> {
                List<String> l2 = (ArrayList<String>) context.getData("foo");
                l2.add("lPolicyC");
                return n.process();
            });
            //
            return next.process();
        });
        // Store a data in context
        cxt.setData("foo", new ArrayList<String>());
        //
        Mono<HttpResponse> responseMono = pipeline.sendRequest(cxt);
        responseMono.block();
        //
        List<String> l = (ArrayList<String>) cxt.getData("foo");
        Assert.assertNotNull(l);
        Assert.assertEquals(5, l.size());
        Assert.assertEquals("lPolicyA", l.get(0));
        Assert.assertEquals("gPolicyA", l.get(1));
        Assert.assertEquals("lPolicyC", l.get(2));
        Assert.assertEquals("lPolicyB", l.get(3));
        Assert.assertEquals("httpClient", l.get(4));
        // ------> [Ensure global policies are not changed]

        // Create a context for the second http request
        cxt = pipeline.newContext(httpRequest);
        // Store a data in context
        cxt.setData("foo", new ArrayList<String>());
        //
        responseMono = pipeline.sendRequest(cxt);
        responseMono.block();
        //
        l = (ArrayList<String>) cxt.getData("foo");
        Assert.assertNotNull(l);
        Assert.assertEquals(3, l.size());
        Assert.assertEquals("gPolicyA", l.get(0));
        Assert.assertEquals("gPolicyB", l.get(1));
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
