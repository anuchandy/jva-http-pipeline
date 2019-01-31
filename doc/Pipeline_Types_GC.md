### V3 Garbage collection of pipeline and associated types

![alt text](https://github.com/anuchandy/jva-http-pipeline/blob/immutable-pipeline/doc/GC_Ref_count.jpg)


<table>
  <tr>
    <th>Object Type</th>
    <th>Ref_Count</th>
    <th>Notes</th>
  </tr>
      <tr>
        <td>RequestPolicy[]</td><td>count(pipeline_instances)</td><td>no: of pipeline instances created with policies</td>
      </tr>
      <tr>
        <td>HttpPipeline</td><td>count(PipelineCallContext_instances)</td><td>no: of context instances associated with the pipeline</td>
      </tr>
      <tr>
        <td>HttpRequest</td><td>1 [PipelineCallContext.request]</td><td></td>
      </tr>
      <tr>
        <td>NextPolicy</td><td>count(RequestPolicy_instances)</td><td>Second argument of RequestPolice::process is reference to NextPolicy</td>
      </tr>
      <tr>
        <td>PipelineCallContext</td><td>count(RequestPolicy_instances)</td><td>First argument of RequestPolice::process is reference to PipelineCallContext</td>
      </tr>
</table>


[Next: Autorest Java runtime and modules for transport](https://github.com/anuchandy/jva-http-pipeline/blob/immutable-pipeline/doc/Runtime_Transport_Layers.md)
