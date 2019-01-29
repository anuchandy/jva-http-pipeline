
### V3 Pipeline & Request Policies Object allocation

![alt text](https://github.com/anuchandy/jva-http-pipeline/blob/immutable-pipeline/doc/req_policy_cxt.jpg)

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

[Next: Context flow through pipeline & call-stack](https://github.com/anuchandy/jva-http-pipeline/blob/immutable-pipeline/doc/Pipeline_Policies_Flow.md)
