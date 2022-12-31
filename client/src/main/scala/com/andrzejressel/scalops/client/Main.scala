package com.andrzejressel.scalops.client

import com.andrzejressel.scalops.common.ScalopsAPI.*
import com.andrzejressel.scalops.ipc.core.ArrayImplicits.given
import com.andrzejressel.scalops.ipc.core.*
import com.andrzejressel.scalops.ipc.lambda.Lambda.*
import zio.schema.Schema
import zio.{Scope, ZIO, ZIOAppArgs, ZIOAppDefault}

import java.io.ByteArrayInputStream

object Main extends ZIOAppDefault {

  override def run: ZIO[Environment & ZIOAppArgs & Scope, Any, Any] = {
    val realStdErr = System.err
    System.setErr(System.out)

    for {
      p <- zio.Promise.make[
        Nothing,
        Endpoint {
          type USER_INPUT_TYPES = EXECUTIONS2;
          type HANDLER_TYPES    = EXECUTIONS1;
        }
      ]
      classLoader = HandlerClassLoader(p)
      handlers = HandlerBuilder
        .create[EXECUTIONS1]
        .addLambdaHandler(classLoader)
        .handle[DOUBLE_SECRET_NUMBER](number =>
          (for {
            api          <- p.await
            secretNumber <- api.execute[GET_SECRET_NUMBER](())
          } yield (secretNumber * 2)).orDie
        )
        .build()
      _          <- zio.Console.printLine("Client: Setting connection")
      connection <- Connection(System.in, realStdErr)
      api        <- createEndpoint1[API](handlers, connection)
      _          <- p.complete(ZIO.succeed(api))
      _          <- api.looper()
    } yield ()

  }

}
