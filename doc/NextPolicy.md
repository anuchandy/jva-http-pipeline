### NextPolicy type

``RequestPolicy::process(PipelineCallContext cxt, NextPolicy next)``

The task of discovering the next `RequestPolicy` is offloaded to `NextPolicy` type. 

With this choice, `NextPolicy` can be the extensibility point. 

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

<br/>
<br/>
<br/>
<br/>
<br/>
<br/>
Extensibility point: In future, if we have to support mutating pipeline dynamically (e.g. add a policy from inside a policy) then next policy to run cannot be defined statically. `NextPolicy` logic can be then updated accordingly for dynamically lookup.

<br/>
<br/>

[Next: Context flow through pipeline & call-stack](https://github.com/anuchandy/jva-http-pipeline/blob/immutable-pipeline/doc/Pipeline_Policies_Flow.md)

