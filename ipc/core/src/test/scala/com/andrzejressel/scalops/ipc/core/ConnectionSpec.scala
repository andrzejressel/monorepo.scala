package com.andrzejressel.scalops.ipc.core

import zio.*
import zio.test.TestAspect.{
  forked,
  nonFlaky,
  timed,
  timeout,
  withLiveConsole,
  withLiveEnvironment
}
import zio.test.*

import java.io.{
  ByteArrayInputStream,
  ByteArrayOutputStream,
  PipedInputStream,
  PipedOutputStream
}
import scala.collection.immutable.ArraySeq

object ConnectionSpec extends ZIOSpecDefault {

  def spec: Spec[TestEnvironment & Scope, Any] = suite("ConnectionSpec")(
    test("Get next") {
      val byteArray = Array[Byte](0, 0, 0, 1, 127)
      val in        = ByteArrayInputStream(byteArray)
      val out       = ByteArrayOutputStream()

      for {
        connection <- Connection(in, out)
        data       <- connection.getNext
      } yield assertTrue(data sameElements Array(127))
    },
    test("Sent messages can be received") {
      check(Gen.int(1, 4000)) { size =>
        val message      = scala.util.Random.nextBytes(size)
        val inputStream  = PipedInputStream()
        val outputStream = FlushingPipedOutputStream(inputStream)
        for {
          connection <- Connection(inputStream, outputStream)
          _ <- connection.send(ArraySeq.unsafeWrapArray(message)).forkScoped
          received <- connection.getNext
        } yield assertTrue(received sameElements message)
      }
    },
    test("Messages can be pushed through multiple threads") {
      val messageSize = 2000
      val messages =
        (1 to 10)
          .map(_ => scala.util.Random.nextBytes(messageSize))
          .map(_.toSeq)
      val inputStream  = PipedInputStream(50)
      val outputStream = FlushingPipedOutputStream(inputStream)

      for {
        connection <- Connection(inputStream, outputStream)
        _ <- ZIO
          .foreachPar(messages)(m => connection.send(m))
          .forkScoped
        receivedMessages <- ZIO
          .foreach(messages)(_ => connection.getNext.map(_.toSeq))
      } yield assertTrue(receivedMessages.toSet == messages.toSet)
    } @@ nonFlaky(20)
  ) @@ withLiveConsole @@ timeout(30.second)

}
