### HttpRequest composes Cxt vs Cxt composes HttpRequest


When associating HttpRequest instance with PipelineCallContext instance, we explored following two options:

1. Composing Context in HttpRequest
2. Composing HttpRequest in Context

We decided to choose #2 due to following reasons:

A. We want runtime HttpRequest to look as close as http request defined by HTTP protocol.
    HttpRequest contains only headers and content (body).

B. Wanted to keep PipelineCallContext as future extensibility point.
  
  e.g. for B, We did some prototype where 
  1. Pipeline defines global policies which applied across all requests goes through the pipeline. 
  2. In-addition to B.1 there is also an option to apply another set policies only to arbitrary request.
        Since there is 1:1 association between context and request, we exposed methods in contexts to operate on these type of policies.
        
  Today there is no real use case for this, but if any requirement like this come up then we can support it without breaking.
  
