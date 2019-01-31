
## Request and Response Content


The type we pick for request and response 'content' is 	extremely important as plays THE major role in GC pressure.

In this section we explore various available buffer types, pros and cons of each and finally the discuss the buffer type for request-response content. 

* java.nio.buffer.ByteBuffer
* io.netty.buffer.ByteBuf
* reactor.netty.ByteBufFlux

### java.nio.buffer.ByteBuffer

JDK built-in type that simplifies operation on the byte content.

Two types of ByteBuffer:

  1. Direct 
  2. Non-direct
    
 | Direct  | Non-Direct |
 | ------------- | ------------- |
 | 1. Backing memory is native (not managed by GC).    | Backing memory is in heap and managed by GC.  |
 | 2. JVM will make 'best effort' to perform native I/O directly on it. |   |
 | 3. Higher allocation and de-allocation cost.     |   |
 

*'Direct native IO': JDK do best effort to avoid copying buffer content to [or] from intermediate buffer before [or] after the invocation of OS native IO api.*


### io.netty.buffer.ByteBuf

*The runtime is built on the top of reactor-netty. Reactor-netty enables reactive programming on Netty.*

`io.netty.buffer.ByteBuf`: The Netty’s type that wraps JDK ```java.nio.buffer.ByteBuffer``` & simplifies operation on buffer.

Starting from 4.x - Netty supports pooling (ref count based) of ```io.netty.buffer.ByteBuf```. 
There are 4 concrete implementation of  ```io.netty.buffer.ByteBuf```.

| io.netty.buffer.ByteBuf Impl | Allocated From | pooled/un-pooled |
|------|--------------- |--------------- |
|io.netty.buffer.PooledDirectByteBuf | native_memory | pooled
|io.netty.buffer.PooledHeapByteBuf  | heap_memory | pooled
|io.netty.buffer.io.netty.buffer.UnpooledDirectByteBuf  | native_memory | un-pooled
|io.netty.buffer.UnpooledHeapByteBuf  | heap_memory | un-pooled


#### io.netty.buffer.ByteBuf Pooling performance

Twitter did [perf test](https://blog.twitter.com/engineering/en_us/a/2013/netty-4-at-twitter-reduced-gc-overhead.html) on Netty’s pooling.

 |  | Without pooling | With pooling |
 |------|--------------- | --------------- |
 |GC Pauses| ~45 times per min | ~9 times per min |
 |Garbage production| ~207 MB/sec | ~41 MB/sec |
 |Allocation speed | | as the size increases, allocating from buffer pool is much faster than JVM buffer allocation.  |
 
![alt text](https://github.com/anuchandy/jva-http-pipeline/blob/immutable-pipeline/doc/netty_4_at_twitterreducedgcoverhead95.thumb.1280.1280.png)

*courtesy for above image [twitter](https://blog.twitter.com/engineering/en_us/a/2013/netty-4-at-twitter-reduced-gc-overhead.html)*

#### Pooling caveat: 
    
We cannot rely on GC to return the unused buffers into the pool, we are responsible for releasing the buffer, so be very careful about leaks.

Good thing: Netty has a simple leak reporting facility that should help to identify and fix the leaks.

#### More on netty buffer: 
    https://github.com/anuchandy/jva-http-pipeline/blob/immutable-pipeline/doc/Netty-ByteBuf.docx

### reactor.netty.ByteBufFlux

```reactor.netty.ByteBufFlux```: Type defined by reactor-netty representing a stream of `io.netty.buffer.ByteBuf` (`Flux<io.netty.buffer.ByteBuf>`).

#### Response content

In reactor-netty, the response content from service is represented using `reactor.netty.ByteBufFlux` type. As user consumes each chunk (`io.netty.buffer.ByteBuf`) in this stream, reactor-netty takes care of releasing consumed ByteBuf to Netty's pool.

There is no additional allocation of ByteBuf other than the one netty allocate from it's pool.

Today the [content()](https://github.com/Azure/autorest-clientruntime-for-java/blob/996c7b706875293858e91a1f1a14330334bc88b9/client-runtime/src/main/java/com/microsoft/rest/v3/http/NettyClient.java#L120) getter property in our `Runtime Response` type returns `Flux<java.nio.buffer.ByteBuffer>`.

1. We convert `Flux<io.netty.buffer.ByteBuf>` from reactor-netty to `Flux<java.nio.buffer.ByteBuffer>`, which requires copying. This is less efficient. 
2. `io.netty.buffer.ByteBuf` is backed by pooled `java.nio.buffer.ByteBuffer`, it is not recommended to provide direct access to this reference-counted backing array. [Netty doc discourages it].


TODO: We may end up having `content()` returns `ByteBufFlux`/`Flux<io.netty.buffer.ByteBuf>`.
 
#### Request content

In the core, netty requires the type of content to write to the wire is of `io.netty.buffer.ByteBuf`.

Reactor-netty expose `send(..)` APIs that takes `Flux<io.netty.buffer.ByteBuf>` or `ByteBufFlux`.

Today the `content()` property in our `runtime Request` type is of type `Fluex<java.nio.buffer.ByteBuffer>`.

1. We [wrap](https://github.com/Azure/autorest-clientruntime-for-java/blob/996c7b706875293858e91a1f1a14330334bc88b9/client-runtime/src/main/java/com/microsoft/rest/v3/http/NettyClient.java#L86) `Fluex<java.nio.buffer.ByteBuffer>` in un-pooled `Flux<io.netty.buffer.ByteBuf>` and give it to reactor-netty.

TODO: We may want give more control to user where he can prepare `Flux<io.netty.buffer.ByteBuf>` using either Netty's pooled or un-pooled allocator.
In this case user don't have to worry about releasing of these ByteBuf to pool. 
Based on Netty reference-count guideline, its the responsibility of the receiving-component to release ByteBuf.
Ref: http://netty.io/wiki/reference-counted-objects.html


 






