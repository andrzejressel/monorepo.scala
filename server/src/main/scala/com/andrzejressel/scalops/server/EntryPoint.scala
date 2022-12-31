package com.andrzejressel.scalops.server

import com.andrzejressel.scalops.common.ScalopsAPI.*
import com.andrzejressel.scalops.ipc.core.ArrayImplicits.given_Schema_Array
import com.andrzejressel.scalops.ipc.core.*
import com.andrzejressel.scalops.server.handler.GetClassHandler
import zio.ZIO

import java.io.{InputStream, OutputStream}

object EntryPoint {

  def createApi(is: InputStream, os: OutputStream) = {

    val handlers: Handlers[EXECUTIONS2] =
      HandlerBuilder
        .create[EXECUTIONS2]
        .handle[GET_CLASS](GetClassHandler(_).orDie)
        .handle[GET_SECRET_NUMBER](_ => ZIO.succeed(12))
        .build()

    for {
      connection <- Connection(is, os)
      api        <- createEndpoint2[API](handlers, connection)
      _          <- api.looper().forkScoped
    } yield api
  }

}
