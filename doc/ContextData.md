### Data in PipelineCallContext

Two APIs to operate on data
 
 1. `void PipelineCallContext::addData(String:key, Object:value)` 
 2. `Object PipelineCallContext::getData(String:key)`  

Internally each data (key & value) is stored in an instance of `ContextData`. This is an immutable data structure.

 
![alt text](https://github.com/anuchandy/jva-http-pipeline/blob/immutable-pipeline/doc/ContextData.png)
 

<br/>
<br/>

#### Immutability of ContextData enables data sharing

```java
// Create data with 3 key-value pairs
ContextData data = new ContextData("key1", "val1")
    .addData("key2", "val2")
    .addData("key3", "val3");

// Share the data across two requests that can possibly run in parallel.
//

// Data Share 1
PipelineCallContext cx1 = pipeline.newContext(HttpRequest: request1, ContextData: data);
Mono<HttpResponse> monoResponse1 = pipeline.sendRequest(cxt1);

// Data Share 2
PipelineCallContext cx2 = pipeline.newContext(HttpRequest: request2, ContextData: data);
Mono<HttpResponse> monoResponse2 = pipeline.sendRequest(cxt2);

// Merge Mono to enable parallel execution
Flux<HttpResponse> fluxResponses = monoResponse1.mergeWith(monoResponse2);

// Subscribe to kick of parallel execution
fluxResponses.subscribe(httpResponse -> {
    // process response
});  

```

[Next: Autorest Java runtime and modules for transport](https://github.com/anuchandy/jva-http-pipeline/blob/immutable-pipeline/doc/Runtime_Transport_Layers.md)
