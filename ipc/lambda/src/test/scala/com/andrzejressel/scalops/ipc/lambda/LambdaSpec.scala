package com.andrzejressel.scalops.ipc.lambda

import com.andrzejressel.scalops.ipc.core.ArrayImplicits.given
import com.andrzejressel.scalops.ipc.core.*
import org.apache.commons.io.input.CountingInputStream
import org.apache.commons.io.output.CountingOutputStream
import zio.*
import zio.schema.codec.JsonCodec
import zio.test.TestAspect.*
import zio.test.*

import java.io.{
  ByteArrayInputStream,
  ByteArrayOutputStream,
  PipedInputStream,
  PipedOutputStream
}
import scala.collection.immutable.ArraySeq

import Lambda.*
object AnyFunctionSpec extends ZIOSpecDefault {

  def spec: Spec[TestEnvironment & Scope, Any] = suite("AnyFunctionSpec")(
    test("Should work with function without arguments") {

      type EXECUTIONS1 = EmptyTuple
      type EXECUTIONS2 = LAMBDA_EXECUTION *: EmptyTuple
      type API         = API_DECLARATION[EXECUTIONS1, EXECUTIONS2]

      val is1 = PipedInputStream(50)
      val is2 = PipedInputStream(50)
      val os1 = FlushingPipedOutputStream(is1)
      val os2 = FlushingPipedOutputStream(is2)

      val handlers: Handlers[EXECUTIONS2] =
        HandlerBuilder
          .create[EXECUTIONS2]
          .addLambdaHandler(getClass().getClassLoader())
          .build()

      for {
        c1   <- Connection(is1, os2)
        c2   <- Connection(is2, os1)
        api1 <- createEndpoint1[API](EMPTY_HANDLER, c1)
        api2 <- createEndpoint2[API](handlers, c2)
        _    <- api1.looper().forkScoped
        _    <- api2.looper().forkScoped

        result <- api1.runFunction(() => 123)
      } yield assertTrue(result == 123)
    },
    test("Should work with function with arguments") {

      type EXECUTIONS1 = EmptyTuple
      type EXECUTIONS2 = LAMBDA_EXECUTION *: EmptyTuple
      type API         = API_DECLARATION[EXECUTIONS1, EXECUTIONS2]

      val is1 = PipedInputStream(50)
      val is2 = PipedInputStream(50)
      val os1 = FlushingPipedOutputStream(is1)
      val os2 = FlushingPipedOutputStream(is2)

      val handlers: Handlers[EXECUTIONS2] =
        HandlerBuilder
          .create[EXECUTIONS2]
          .addLambdaHandler(getClass().getClassLoader())
          .build()

      for {
        c1   <- Connection(is1, os2)
        c2   <- Connection(is2, os1)
        api1 <- createEndpoint1[API](EMPTY_HANDLER, c1)
        api2 <- createEndpoint2[API](handlers, c2)
        _    <- api1.looper().forkScoped
        _    <- api2.looper().forkScoped

        result <- api1.runFunction("abc")((s) => s + "xyz")
      } yield assertTrue(result == "abcxyz")
    }
  ) @@ timeout(30.second)

}
