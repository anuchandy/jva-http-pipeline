package com.azjvsdk.experimental.http;

import com.azjvsdk.experimental.http.pipeline.HttpPipeline;
import com.azjvsdk.experimental.http.pipeline.HttpPipelinePolicy;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Rule;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;

public class HttpRetryPolicyTests {
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8089);



    @Test
    public void exampleTest() throws IOException {
        //
        WireMock.stubFor(get(urlEqualTo("/an/endpoint"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "text/plain")
                        .withStatus(200)
                        .withBody("You've reached a valid WireMock endpoint")));

        HttpRequest httpRequest = new HttpRequest("", HttpMethod.GET,
                new URL("http://localhost:8089//an/endpoint"));
        //
        HttpPipeline pipeline = new HttpPipeline(new HttpPipelinePolicy[0], new ApacheHttpClient());
        pipeline.sendRequest(httpRequest).block();

        //
        List<ServeEvent> allServeEvents = WireMock.getAllServeEvents();
        System.out.println(allServeEvents.size());
        //
    }

    @Test
    public void foo() {
        ByteBuffer buf = ByteBuffer.wrap("Cancel newspaper subscription".getBytes(Charset.defaultCharset()));
        ByteBuf bBuf = Unpooled.wrappedBuffer(buf);
        CompositeByteBuf cBuf = Unpooled.compositeBuffer();

        cBuf.addComponent(true, bBuf);
        int length = cBuf.readableBytes();
        System.out.println(cBuf.readableBytes());

        byte[] byteArray = new byte[length];
        cBuf.getBytes(cBuf.readerIndex(), byteArray);
        System.out.println(new String(byteArray, Charset.defaultCharset()));

//        int length = bBuf.readableBytes();
//        byte[] byteArray = new byte[length];
//        bBuf.getBytes(bBuf.readerIndex(), byteArray);
//        System.out.println(new String(byteArray, Charset.defaultCharset()));

        //
//        CompositeByteBuf cBuf = Unpooled.compositeBuffer();
//        cBuf.addComponent(Unpooled.wrappedBuffer(buf));
//        //
//
//        int length = cBuf.readableBytes();
//        byte[] byteArray = new byte[length];
//        cBuf.getBytes(cBuf.readerIndex(), byteArray);
//        System.out.println(new String(byteArray, Charset.defaultCharset()));


       // System.out.println(new String(buf.array(), Charset.defaultCharset()));


    }

    @Test
    public void exampleTest1() throws IOException {
        WireMock.stubFor(get(urlEqualTo("/todo/items")).inScenario("To do list")
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse()
                        .withBody("<items>" +
                                "   <item>Buy milk</item>" +
                                "</items>")));

        WireMock.stubFor(post(urlEqualTo("/todo/items")).inScenario("To do list")
                .whenScenarioStateIs(STARTED)
                .withRequestBody(containing("Cancel newspaper subscription"))
                .willReturn(aResponse().withStatus(201))
                .willSetStateTo("Cancel newspaper item added"));

        WireMock.stubFor(get(urlEqualTo("/todo/items")).inScenario("To do list")
                .whenScenarioStateIs("Cancel newspaper item added")
                .willReturn(aResponse()
                        .withBody("<items>" +
                                "   <item>Buy milk</item>" +
                                "   <item>Cancel newspaper subscription</item>" +
                                "</items>")));

        HttpPipeline pipeline = new HttpPipeline(new HttpPipelinePolicy[0], new ApacheHttpClient());
        //
        //
        HttpRequest httpRequest1 = new HttpRequest("", HttpMethod.GET,
                new URL("http://localhost:8089/todo/items"));
       Mono<HttpResponse> response1 = pipeline.sendRequest(httpRequest1);

       String responseBody1 = response1
               .flatMap(r -> Mono.just(new String(byteBufferToArray(r.body()), Charset.defaultCharset())))
               .block();

        System.out.println(responseBody1);

        ByteBuffer buf = ByteBuffer.wrap("Cancel newspaper subscription".getBytes(Charset.defaultCharset()));
        Flux<ByteBuffer> body = Flux.<ByteBuffer>just(buf);
        HttpRequest httpRequest2 = new HttpRequest(HttpMethod.POST,
                new URL("http://localhost:8089/todo/items"), new HttpHeaders(), body);
        //
        Mono<HttpResponse> response2 = pipeline.sendRequest(httpRequest2);

        String responseBody2 = response2
                .flatMap(r -> Mono.just(new String(byteBufferToArray(r.body()), Charset.defaultCharset())))
                .block();

        System.out.println(responseBody2);
        //
        //
        HttpRequest httpRequest3 = new HttpRequest("", HttpMethod.GET,
                new URL("http://localhost:8089/todo/items"));
        Mono<HttpResponse> response3 = pipeline.sendRequest(httpRequest3);

        String responseBody3 = response3
                .flatMap(r -> Mono.just(new String(byteBufferToArray(r.body()), Charset.defaultCharset())))
                .block();

        System.out.println(responseBody3);
    }

    private static byte[] byteBufferToArray(Flux<ByteBuffer> stream) {
        return stream.collect(() -> Unpooled.compositeBuffer(), (compBuffer, buffer) -> {
            compBuffer.addComponent(true, Unpooled.wrappedBuffer(buffer));
        })
        .map(compBuffer -> {
            int length = compBuffer.readableBytes();
            byte[] byteArray = new byte[length];
            compBuffer.getBytes(compBuffer.readerIndex(), byteArray);
            return byteArray;
        }).block();
    }

}
