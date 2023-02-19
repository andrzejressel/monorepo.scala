package com.andrzejressel.scalops.client

import com.andrzejressel.scalops.common.ScalopsAPI.*
import com.andrzejressel.scalops.ipc.core.Endpoint
import zio.Duration.*
import zio.*

class HandlerClassLoader(
    val apiPromise: Promise[
      Nothing,
      CLIENT_ENDPOINT
    ]
) extends ClassLoader() {

  override def findClass(name: String): Class[?] = {
    val array = _run((for {
      api            <- apiPromise.await
      byteCodeOption <- api.execute[GET_CLASS](name)
      byteCode <- ZIO
        .fromOption(byteCodeOption)
        .map(_.toArray)
        .mapError(_ => IllegalStateException(s"Cannot find class ${name}"))
    } yield byteCode).orDie)
    println(s"Creating class: ${name}")
    // DefineClass hangs when called from another thread
    val clz = defineClass(name, array, 0, array.length, null)
    println("Created class")
    clz
  }

  private def _run[A](z: ZIO[Any, Nothing, A]) =
    zio.Unsafe.unsafely(
      zio.Runtime.default.unsafe
        .run(z)
        .getOrThrow()
    )

}
