### Pipeline default policies

There will be default policies in the pipeline, such as:

1. RetryPolicies for various retry strategies
2. TelemetryPolicy
3. LoggerPolicy

Once we decide on these and have them implemented, we will expose another ``HttpPipeline`` ctr that takes an object composing these defaults. 