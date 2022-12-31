package com.andrzejressel.scalops.client

import com.andrzejressel.scalops.common.ScalopsAPI.*
import com.andrzejressel.scalops.ipc.core.Endpoint
import zio.Duration.*
import zio.*

class HandlerClassLoader(
    val apiPromise: Promise[
      Nothing,
      Endpoint {
        type USER_INPUT_TYPES = EXECUTIONS2; type HANDLER_TYPES = EXECUTIONS1
      }
    ]
) extends ClassLoader() {

  override def findClass(name: String): Class[?] = {
    val array = _run((for {
      api            <- apiPromise.await
      byteCodeOption <- api.execute[GET_CLASS](name)
      _ <- zio.Console.printLine(s"Found bytecode: ${byteCodeOption.isDefined}")
      byteCode <- ZIO.fromOption(byteCodeOption).map(_.toArray)
    } yield byteCode).orDieWith { obj =>
      println(obj); throw RuntimeException(s"$obj")
    })
    println(s"Creating class: ${name}")
    // DefineClass hangs when called from another thread
    val clz = defineClass(name, array, 0, array.length, null)
    println("Created class")
    clz
  }

  def rethrowErrors[A](f: () => A): A = {
    try {
      f()
    } catch {
      case (e: Error) => throw RuntimeException(e)
    }
  }

  private def _run[A](z: ZIO[Any, Nothing, A]) = zio.Unsafe
    .unsafely(
      zio.Runtime.default.unsafe
        .run(z)
        .getOrThrow()
    )

}
