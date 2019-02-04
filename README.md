# jva-http-pipeline [V3 proposal]

## Types in pipeline

1. RequestPolicy
2. HttpPipeline
3. PipelineCallContext
4. NextPolicy

### RequestPolicy

```java
@FunctionalInterface
public interface HttpPipelinePolicy {
    /**
     * Process provided request context and invokes the next policy.
     *
     * @param context request context
     * @param next the next policy to invoke
     * @return publisher that initiate the request upon subscription and emits response on completion.
     */
    Mono<HttpResponse> process(HttpPipelineCallContext context, NextPolicy next);
}
```

### HttpPipeline

Pipeline is created with policy instances.

```java
HttpPipelinePolicy[] policies = getRequestPolicies();
HttpClient httpClient = createHttpClient();
//
HttpPipeline pipeline = new HttpPipeline(HttpPipelinePolicy[]: policies, HttpClient: httpClient);
```
Same pipeline can be used for multiple HttpRequests, means the same policies are applied on those HttpRequests and corresponding HttpResponses.

[Pipeline default policies](https://github.com/anuchandy/jva-http-pipeline/blob/immutable-pipeline/doc/DefaultPolicies.md)

### PipelineCallContext

Each HttpRequest instance must have a unique context associated with it.

```java
// Create http request
HttpRequest httpRequest = createHttpRequest();
// Create a context for the http request
HttpPipelineCallContext cxt = pipeline.newContext(HttpRequest: httpRequest);
```
The context is used by policies in the pipeline to:
 1. Access the request
 2. Store and retrieve data

[HttpRequest composes Cxt vs Cxt composes HttpRequest](https://github.com/anuchandy/jva-http-pipeline/blob/immutable-pipeline/doc/RequestInContext_vs_ContextInRequest.md)

###  Example: Request policies, HttpPipeline, PipelineCallContext & HttpRequest

```java
// Create http client
HttpClient httpClient = createHttpClient();

// HttpPipelinePolicies
HttpPipelinePolicy[] policies = new HttpPipelinePolicy[2];

// First policy
policies[0] = (context, next) -> {
    // logic to process request, context.request()
    // use context.setData(string:key, Object:value), context.getData(string:key) to access state
    Mono<HttpResponse> monoResponse = next.process();
    return monoResponse.map(response -> {
        // logic to process HttpResponse
        return response;
    });
};

// Second policy
policies[1] = (context, next) -> {
    // ...
    return next.process();
};

// Create pipeline
HttpPipeline pipeline = new HttpPipeline(policies, httpClient);


// Create first http request
HttpRequest httpRequest0 = createHttpRequest0();
Mono<HttpResponse> responseMono = pipeline.sendRequest(httpRequest0);


// Create second http request
HttpRequest httpRequest1 = createHttpRequest1();
//
HttpPipelineCallContext cxt1 = pipeline.newContext(httpRequest1);
cxt1.setData("azure", "awesome");
// send the context through pipeline
Mono<HttpResponse> responseMono = pipeline.sendRequest(cxt1);

```
