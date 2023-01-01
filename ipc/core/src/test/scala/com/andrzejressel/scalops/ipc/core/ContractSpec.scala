package com.andrzejressel.scalops.ipc.core

import com.andrzejressel.scalops.ipc.core.ArrayImplicits.given
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

object ContractSpec extends ZIOSpecDefault {

  def spec: Spec[TestEnvironment & Scope, Any] = suite("ContractSpec")(
    test("Contract should work") {

      type TO_INT_EXECUTION = Execution["toInt", String, Int]
      type DOUBLE_EXECUTION = Execution["double", Int, Int]
      type EXECUTIONS1      = EmptyTuple
      type EXECUTIONS2      = TO_INT_EXECUTION *: DOUBLE_EXECUTION *: EmptyTuple
      type API              = API_DECLARATION[EXECUTIONS1, EXECUTIONS2]

      val is1 = PipedInputStream(50)
      val is2 = PipedInputStream(50)
      val os1 = FlushingPipedOutputStream(is1)
      val os2 = FlushingPipedOutputStream(is2)

      val handlers: Handlers[EXECUTIONS2] =
        HandlerBuilder
          .create[EXECUTIONS2]
          .handle[TO_INT_EXECUTION](s => ZIO.succeed(Integer.parseInt(s)))
          .handle[DOUBLE_EXECUTION](i => ZIO.succeed(i * 2))
          .build()

      for {
        c1   <- Connection(is1, os2)
        c2   <- Connection(is2, os1)
        api1 <- createEndpoint1[API](EMPTY_HANDLER, c1)
        api2 <- createEndpoint2[API](handlers, c2)
        _    <- api1.looper().forkScoped
        _    <- api2.looper().forkScoped

        result1 <- api1.execute[TO_INT_EXECUTION]("12")
        result2 <- api1.execute[DOUBLE_EXECUTION](result1)
      } yield assertTrue(result1 == 12, result2 == 24)
    },
    test("Overhead of sending byte arrays should be less than 10%") {

      type EXECUTION   = Execution["byteArray", Array[Byte], Int]
      type EXECUTIONS1 = EmptyTuple
      type EXECUTIONS2 = EXECUTION *: EmptyTuple
      type API         = API_DECLARATION[EXECUTIONS1, EXECUTIONS2]

      val is1 = PipedInputStream(50)
      val is2 = PipedInputStream(50)
      val os1 = FlushingPipedOutputStream(is1)
      val os2 = CountingOutputStream(FlushingPipedOutputStream(is2))

      val handlers: Handlers[EXECUTIONS2] =
        HandlerBuilder
          .create[EXECUTIONS2]
          .handle[EXECUTION](s => ZIO.succeed(s.length))
          .build()

      val hugeArray = Array.fill[Byte](1_000_000)(0)

      for {
        c1   <- Connection(is1, os2)
        c2   <- Connection(is2, os1)
        api1 <- createEndpoint1[API](EMPTY_HANDLER, c1)
        api2 <- createEndpoint2[API](handlers, c2)
        _    <- api1.looper().forkScoped
        _    <- api2.looper().forkScoped

        result <- api1.execute[EXECUTION](hugeArray)
      } yield assertTrue(
        os2.getCount() > 1_000_000,
        os2.getCount() < 1_100_000
      )

    }
  ) @@ timeout(30.second)

}
