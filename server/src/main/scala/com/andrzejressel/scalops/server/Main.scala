package com.andrzejressel.scalops.server

import com.andrzejressel.scalops.common.ScalopsAPI.*
import com.andrzejressel.scalops.ipc.core.{
  EMPTY_HANDLER,
  HandlerBuilder,
  Handlers
}
import zio.schema.Schema
import zio.schema.Schema.*
import zio.{Chunk, Scope, ZIO, ZIOAppArgs, ZIOAppDefault}

import handler.GetClassHandler

object Main extends ZIOAppDefault {

  override def run: ZIO[Environment | ZIOAppArgs | Scope, Any, Any] = {

    // val handlers: Handlers[EXECUTIONS2] =
    // HandlerBuilder
    // .create[EXECUTIONS2]
    // FIXME
    // .handle[GET_NUMBER_EXECUTION](GetClassHandler.andThen(_.get))
    // .build()

    ZIO.unit
  }
}
