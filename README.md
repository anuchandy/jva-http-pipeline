# jva-http-pipeline [V3 proposal]

## RequestPolicy interface

```java
@FunctionalInterface
public interface RequestPolicy {
    /**
     * Process provided request context and invokes the next policy.
     *
     * @param context request context
     * @param next the next policy to invoke
     * @return publisher that initiate the request upon subscription and emits response on completion.
     */
    Mono<HttpResponse> process(PipelineCallContext context, NextPolicy next);
}
```

##  HttpPipeline and policies

Pipeline is created with policy instances.

```java
RequestPolicy[] policies = getRequestPolicies();
HttpPipeline pipeline = new HttpPipeline(policies, httpClient);
```
## The PipelineCallContext

Each HttpRequest instance must have a unique context associated with it.

```java
// Create http request
HttpRequest httpRequest0 = createHttpRequest0();
// Create a context for the http request
PipelineCallContext cxt0 = pipeline.newContext(httpRequest0);
```

##  HttpPipeline, policies, context & requests

Same pipeline can be used for multiple HttpRequests, means the same policies are applied on those HttpRequests and corresponding HttpResponses.

```java
// Create http client
HttpClient httpClient = createHttpClient();
RequestPolicy[] policies = new RequestPolicy[2];
//
// First policy
policies[0] = (context, next) -> {
    // logic to process request, context.request()
    // use context.setData(string:key, Object:value), context.getData(string:key) to access state
    Mono<HttpResponse> monoResponse = next.process();
    return mono.map(response -> {
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
// Create a context for the first http request & send it
Mono<HttpResponse> responseMono = pipeline.sendRequest(pipeline.newContext(httpRequest0));
// Create second http request
HttpRequest httpRequest1 = createHttpRequest1();
// Create a context for the second http request & send it
Mono<HttpResponse> responseMono = pipeline.sendRequest(pipeline.newContext(httpRequest1));

```

<table>
  <tr>
    <th>Reference</th>
  </tr>
  <tr>
    <td><li><a href="https://github.com/anuchandy/jva-http-pipeline/blob/immutable-pipeline/doc/V3_Pipeline_Policy_Object_Allocation.md">Pipeline & policies object allocation</a></li></td>
  </tr>
</table>

