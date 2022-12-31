package com.andrzejressel.scalops.integration

import com.andrzejressel.scalops.ipc.lambda.Lambda.*
import com.andrzejressel.scalops.ipc.core.*
import com.andrzejressel.scalops.common.ScalopsAPI.*
import zio.*
import zio.test.TestAspect.{
  fibers,
  forked,
  ignore,
  nonFlaky,
  timed,
  timeout,
  withLiveClock,
  withLiveConsole,
  withLiveEnvironment
}
import zio.test.{Spec, TestEnvironment, ZIOSpecDefault, assertTrue}
import zio.process.{Command, CommandError, ProcessInput, ProcessOutput}
import zio.stream.ZStream

import java.io.{
  ByteArrayInputStream,
  InputStream,
  ObjectInputStream,
  ObjectStreamClass
}
import java.nio.file.Paths
import scala.collection.immutable
import scala.util.Try
import com.andrzejressel.scalops.server.EntryPoint

object Integration extends ZIOSpecDefault {

  def spec: Spec[TestEnvironment & Scope, Any] =
    suite("ConnectionSpec")(
      test("Basic function test") {

        val path = Paths.get(
          "client",
          "target",
          "scala-3.2.1",
          "client-assembly-0.1.0-SNAPSHOT.jar"
        )

        for {
          commandQueue <- Queue.unbounded[Chunk[Byte]]
          c <- Command("java", "-jar", path.toAbsolutePath.toString)
            .stdin(ProcessInput.fromQueue(commandQueue))
            .stdout(ProcessOutput.Inherit)
            .run

          _ <- Scope.addFinalizer(
            ZIO.ifZIO(c.isAlive)(c.killForcibly.ignore, ZIO.unit)
          )
          is <- c.execute(_.getErrorStream())
          os = CommandQueueOutputStream(commandQueue)
          api    <- EntryPoint.createApi(is, os)
          number <- api.runFunction(() => "Serialized function")
          _      <- c.kill
        } yield assertTrue(number == "Serialized function")

      } @@ withLiveEnvironment
    ) @@ timeout(10.second)

}
