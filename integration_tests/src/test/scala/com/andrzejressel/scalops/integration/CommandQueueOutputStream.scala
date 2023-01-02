package com.andrzejressel.scalops.integration

import zio.{Chunk, Queue, ZIO}

class CommandQueueOutputStream(commandQueue: Queue[Chunk[Byte]])
    extends java.io.OutputStream {

  override def write(b: Int): Unit =
    _run(commandQueue.offer(Chunk(b.toByte)))

  override def write(b: Array[Byte]): Unit =
    _run(commandQueue.offer(Chunk.fromArray(b)))

  private def _run[A](z: ZIO[Any, Nothing, A]) = zio.Unsafe
    .unsafely(
      zio.Runtime.default.unsafe
        .run(z)
        .getOrThrow()
    )

}
