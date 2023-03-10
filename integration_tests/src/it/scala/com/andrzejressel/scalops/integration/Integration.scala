package com.andrzejressel.scalops.integration

import com.andrzejressel.scalops.common.BuildInfo.scalaVersion
import com.andrzejressel.scalops.common.ScalopsAPI.*
import com.andrzejressel.scalops.ipc.core.*
import com.andrzejressel.scalops.ipc.lambda.Lambda.*
import com.andrzejressel.scalops.server.EntryPoint
import zio.*
import zio.process.{Command, CommandError, ProcessInput, ProcessOutput}
import zio.stream.ZStream
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

import java.io.{
  ByteArrayInputStream,
  InputStream,
  ObjectInputStream,
  ObjectStreamClass
}
import java.nio.file.{Files, Paths}
import scala.collection.immutable
import scala.util.Try

object Integration extends ZIOSpecDefault {

  def spec: Spec[TestEnvironment & Scope, Any] =
    suite("ConnectionSpec")(
      test("Basic function test") {

        val path = Paths.get(
          "client",
          "target",
          s"scala-${scalaVersion}",
          "scalopsClient-assembly-0.1.0-SNAPSHOT.jar"
        )

        for {
          _ <-
            if !Files.exists(path) then {
              ZIO.fail(s"Path [$path] does not exist")
            } else {
              ZIO.unit
            }
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
    ) @@ timeout(30.second)

}
