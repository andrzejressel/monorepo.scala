package com.andrzejressel.scalops.server
package handler

import zio.{Chunk, Task}

import java.io.InputStream

object GetClassHandler extends (String => Task[Option[Chunk[Byte]]]) {
  override def apply(clzName: String): Task[Option[Chunk[Byte]]] = {

    zio.ZIO.attemptBlocking {
      val classFile =
        "/" + clzName.replace('.', '/') + ".class"

      val url: java.net.URL | Null = getClass.getResource(classFile)

      Option
        .apply(url)
        .map(url =>
          Chunk.fromArray(
            (url.getContent.asInstanceOf[InputStream]).readAllBytes()
          )
        )
    }

  }
}
