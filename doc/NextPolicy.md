# NextPolicy type

We considered following two variant of RequestPolicy::process(params) method 

1. ``RequestPolicy::process(PipelineCallContext cxt, RequestPolicy next)``
        <br/>Takes reference to the next `RequestPolicy` directly, i.e. it is statically defined.
        
2. ``RequestPolicy::process(PipelineCallContext cxt, NextPolicy next)``
        <br/> Offload the task of discovering the next `RequestPolicy` to `NextPolicy` type. 
        
We picked variant-2 as future extensibility point. In future, if we have to support mutating pipeline dynamically (e.g. add a policy from inside a policy) then next policy to run cannot be defined statically. `NextPolicy` logic can be then updated accordingly for dynamically lookup.

// Current NextPolicy impl
```java
public class NextPolicy {
    private final PipelineCallContext cxt;

    NextPolicy(final PipelineCallContext cxt) { // Package Private ctr.
        this.cxt = cxt;
    }

    public Mono<HttpResponse> process() {
        return this.cxt.nextPipelinePolicy().process(this.context, this);
    }
}

```

[Next: Context flow through pipeline & call-stack](https://github.com/anuchandy/jva-http-pipeline/blob/immutable-pipeline/doc/Pipeline_Policies_Flow.md)
