# jva-http-pipeline [V3 proposal]


Ref: [pipeline](https://github.com/anuchandy/jva-http-pipeline/tree/master/src/main/java/com/azjvsdk/experimental/http/pipeline),
     [usage](https://github.com/anuchandy/jva-http-pipeline/blob/master/src/test/java/com/azjvsdk/experimental/http/HttpPipelineTests.java)
     
1. Each request has an associated context instance.
2. The policies are stateless & any state will be managed through context instance.
3. Policy implementation just implements policy interface and don’t have to derive from any base policy.
4. Each policy has name associated with it to support lookup operation.
5. Two logical set of policies:
     1. The "global policies" (in pipeline instance level) that gets applied to all request. This is the predefined set of policies that has to be specified while creating pipeline.
     2. The "local policies" (in context instance level) that gets applied only to the request associated with the context.
         1. Application can add, remove, replaces policies through context instance.
         2. Additions, removal & replacement of policies can be done in arbitrary positions (e.g. relative to an existing policy by using name of existing policy to index the position).
         3. A local policy can replace a global policy. Effect is only to the request associated with the context instance.
         4. "local policy operation" can be static or dynamic – 
              1. Static are those gets performed before initiating the request. 
              2. Dynamic are those gets performed during request execution (from within an existing global or local policy).
              
## The PipelineCallContext

Each HttpRequest instance has a unique context associated with it.

```java
// Create http request
HttpRequest httpRequest0 = createHttpRequest0();
// Create a context for the http request
PipelineCallContext cxt0 = pipeline.newContext(httpRequest0);
```

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

##  HttpPipeline and global policies

### Global policies

Pipeline is created with global policies. Same pipeline can be used for multiple HttpRequests, which means the same global policies are applied on those HttpRequests and corrosponding HttpResponses.

```java
// Create http client
HttpClient httpClient = createHttpClient();
RequestPolicy[] globalPolicies = new RequestPolicy[2];
//
// First global policy
globalPolicies[0] = (context, next) -> {
    // logic to process request, context.request()
    // use context.setData(string:key, Object:value), context.getData(string:key) to access state
    Mono<HttpResponse> monoResponse = next.process();
    return mono.map(response -> {
        // logic to process HttpResponse
        return response;
    });
};
// Second global policy
globalPolicies[1] = (context, next) -> {
    // ...
    return next.process();
};
// Create pipeline
HttpPipeline pipeline = new HttpPipeline(globalPolicies, httpClient);
// Create first http request
HttpRequest httpRequest0 = createHttpRequest0();
// Create a context for the first http request & send it
Mono<HttpResponse> responseMono = pipeline.sendRequest(pipeline.newContext(httpRequest0));
// Create second http request
HttpRequest httpRequest1 = createHttpRequest1();
// Create a context for the second http request & send it
Mono<HttpResponse> responseMono = pipeline.sendRequest(pipeline.newContext(httpRequest1));

```

### Global policies can be named

```java
PolicyEntry[] globalPolicyEntries = new PolicyEntry[2];
//
// First named global policy
globalPolicyEntries[0] = new PolicyEntry("gPolicyA", (context, next) -> {
    return next.process();
});
// Second named global policy
globalPolicyEntries[1] = new PolicyEntry("gPolicyB", (context, next) -> {
    return next.process();
});
// Create pipeline
HttpPipeline pipeline = new HttpPipeline(globalPolicies, httpClient);
// Retrieve the global policy entries
PolicyEntry[] entries = pipeline.requestPolicyEntries();
```

##  PipelineCallContext and local policies

Local policies can co-exists with global. The scope of a local policy is limited to a PipelineCallContext instance, means the local policies applied only to the HttpRequest associated with context.

```java
PolicyEntry[] globalPolicyEntries = new PolicyEntry[2];
// First global policy
globalPolicyEntries[0] = new PolicyEntry("gPolicyA", (context, next) -> {
    return next.process();
});
// Second global policy
globalPolicyEntries[1] = new PolicyEntry("gPolicyB", (context, next) -> {
    return next.process();
});
// Create pipeline
HttpPipeline pipeline = new HttpPipeline(globalPolicies, httpClient);
// Create a http request
HttpRequest httpRequest0 = createHttpRequest0();
// Create a context for the request
PipelineCallContext cxt = pipeline.newContext(httpRequest);
// Add first local policy in the beginning
// Policy chain looks like: lPolicyA -> gPolicyA -> gPolicyB
cxt.addPolicyFirst("lPolicyA", (context, next) -> {
    return next.process();
});
// Add a second local policy at the end
// Policy chain looks like: lPolicyA -> gPolicyA -> gPolicyB -> lPolicyB
cxt.addPolicyFirst("lPolicyB", (context, next) -> {
    return next.process();
});
// Add a local policy after 1st global policy
// Policy chain looks like: lPolicyA -> gPolicyA -> lPolicyC -> gPolicyB -> lPolicyB
cxt.addPolicyAfter("gPolicyA", "lPolicyC", (context, next) -> {
    return next.process();
});
// Send the request
Mono<HttpResponse> responseMono = pipeline.sendRequest(cxt);

```

### Local policy can replace a global policy

```java
PolicyEntry[] globalPolicyEntries = new PolicyEntry[1];
// Global policy
globalPolicyEntries[0] = new PolicyEntry("gPolicyA", (context, next) -> {
    return next.process();
});
// Create pipeline
HttpPipeline pipeline = new HttpPipeline(globalPolicies, httpClient);
// Create a http request
HttpRequest httpRequest0 = createHttpRequest0();
// Create a context for the request
PipelineCallContext cxt = pipeline.newContext(httpRequest);
// Replace global policy with a local policy
// Policy chain looks like: lPolicyA ->
cxt.replacePolicy("gPolicyA", "lPolicyA", (context, next) -> {
    return next.process();
});
// Send the request
Mono<HttpResponse> responseMono = pipeline.sendRequest(cxt);

```

### Operations on local policy can be performed from another policy (global or local)

Defer the decision of applying a new policy to another existing policy in the pipeline.

```java
PolicyEntry[] globalPolicyEntries = new PolicyEntry[0];
//
// First global policy
globalPolicyEntries[0] = new PolicyEntry("gPolicyA", (context, next) -> {
    return next.process();
});
// Second global policy
globalPolicyEntries[1] = new PolicyEntry("gPolicyB", (context, next) -> {
    return next.process();
});
// Create http client
HttpClient httpClient = createHttpClient();
// Create pipeline with the two global policies
HttpPipeline pipeline = new HttpPipeline(globalPolicyEntries, httpClient);
// Create http request
HttpRequest httpRequest0 = createHttpRequest();
// Create a context for the http request
PipelineCallContext cxt = pipeline.newContext(httpRequest0);
// Add a local policy in the beginning, when it executes it dynamically change the pipeline
cxt.addPolicyFirst("lPolicyA", (context, next) -> {
    context.replacePolicy("gPolicyB", "lPolicyB", (c, n) -> {
        return n.process();
    });
    context.addPolicyBefore("lPolicyB", "lPolicyC", (c, n) -> {
        return n.process();
    });
    //
    return next.process();
});
// Policy chain looks like: lPolicyA -> gPolicyA -> lPolicyC -> lPolicyB ->
Mono<HttpResponse> responseMono = pipeline.sendRequest(cxt);
```