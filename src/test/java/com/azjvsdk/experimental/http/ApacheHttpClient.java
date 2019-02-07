package com.azjvsdk.experimental.http;

import com.azjvsdk.experimental.http.pipeline.HttpPipelineCallContext;
import io.netty.buffer.Unpooled;
import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class ApacheHttpClient extends HttpClient implements Closeable {
    private final CloseableHttpClient httpClient;

    public ApacheHttpClient() {
        this.httpClient = HttpClients.createDefault();
    }

    @Override
    public Mono<HttpResponse> sendRequestAsync(HttpPipelineCallContext context) {
        return Mono.defer(() -> {
            HttpRequestBase httpRequestBase;
            final String requestUri = context.httpRequest().url().toString();
            if (context.httpRequest().httpMethod() == HttpMethod.GET) {
                httpRequestBase = new HttpGet(requestUri);
            } else if (context.httpRequest().httpMethod() == HttpMethod.DELETE) {
                httpRequestBase = new HttpDelete(requestUri);
            } else if (context.httpRequest().httpMethod() == HttpMethod.HEAD) {
                httpRequestBase = new HttpHead(requestUri);
            } else if (context.httpRequest().httpMethod() == HttpMethod.POST) {
                HttpPost httpPost = new HttpPost(requestUri);
                httpPost.setEntity(byteBufferToEntity(context.httpRequest().body()));
                httpRequestBase = httpPost;
            } else if (context.httpRequest().httpMethod() == HttpMethod.PUT) {
                HttpPut httpPut = new HttpPut(requestUri);
                httpPut.setEntity(byteBufferToEntity(context.httpRequest().body()));
                httpRequestBase = httpPut;
            } else {
                return Mono.error(new IllegalStateException("unknown http-verb:" + context.httpRequest().httpMethod()));
            }
            for (HttpHeader header : context.httpRequest().headers()) {
                httpRequestBase.addHeader(header.name(), header.value());
            }
            return Mono.just(new HttpResponse() {
                @Override
                public int statusCode() {
                    doRequestIfNot();
                    return this.statusCode;
                }

                @Override
                public String headerValue(String headerName) {
                    doRequestIfNot();
                    return this.headers.value(headerName);
                }

                @Override
                public HttpHeaders headers() {
                    doRequestIfNot();
                    return this.headers;
                }

                @Override
                public Flux<ByteBuffer> body() {
                    doRequestIfNot();
                    return Flux.just(this.responseBody);
                }

                //
                //
                private boolean didRequest = false;
                private HttpHeaders headers;
                private ByteBuffer responseBody;
                private int statusCode;
                private void doRequestIfNot() {
                    if (!didRequest) {
                        this.didRequest = true;
                        try (CloseableHttpResponse response = httpClient.execute(httpRequestBase)) {
                            byte[] bytes = readFull(response.getEntity().getContent());
                            this.responseBody = ByteBuffer.wrap(bytes);
                            //
                            this.headers = new HttpHeaders();
                            HeaderIterator itr = response.headerIterator();
                            while (itr.hasNext()) {
                                Header header = itr.nextHeader();
                                headers.set(header.getName(), header.getValue());
                            }
                            //
                            this.statusCode = response.getStatusLine().getStatusCode();
                        } catch (IOException ex) {
                            throw Exceptions.propagate(ex);
                        }
                    }
                }
            });
        });
    }

    @Override
    public void close() throws IOException {
        this.httpClient.close();
    }

    private static byte[] readFull(InputStream inputStream) {
        final int bufferLen = 1024;
        byte[] buffer = new byte[bufferLen];

        try {
            int bytesRead;
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                while ((bytesRead = inputStream.read(buffer, 0, bufferLen)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                return outputStream.toByteArray();
            }
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    private static ByteArrayEntity byteBufferToEntity(Flux<ByteBuffer> stream) {
        return stream.collect(() -> Unpooled.compositeBuffer(), (compBuffer, buffer) -> {
            compBuffer.addComponent(true, Unpooled.wrappedBuffer(buffer));
        })
        .map(compBuffer -> {
            int length = compBuffer.readableBytes();
            byte[] byteArray = new byte[length];
            compBuffer.getBytes(compBuffer.readerIndex(), byteArray);
            return new ByteArrayEntity(byteArray);
        })
        .block();
    }
}
