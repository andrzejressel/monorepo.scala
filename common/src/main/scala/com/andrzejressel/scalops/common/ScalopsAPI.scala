package com.andrzejressel.scalops.common

import com.andrzejressel.scalops.ipc.core.{API_DECLARATION, Execution}
import com.andrzejressel.scalops.ipc.lambda.Lambda.LAMBDA_EXECUTION
import zio.Chunk

import java.util.concurrent.Executor
import com.andrzejressel.scalops.ipc.core.Endpoint

object ScalopsAPI {
  type DOUBLE_SECRET_NUMBER = Execution["addSecret", Unit, Int]
  type GET_SECRET_NUMBER    = Execution["getSecret", Unit, Int]
  type GET_CLASS            = Execution["getClass", String, Option[Chunk[Byte]]]

  type EXECUTIONS1 = LAMBDA_EXECUTION *: DOUBLE_SECRET_NUMBER *: EmptyTuple
  type EXECUTIONS2 = GET_CLASS *: GET_SECRET_NUMBER *: EmptyTuple
  type API         = API_DECLARATION[EXECUTIONS1, EXECUTIONS2]

  type CLIENT_ENDPOINT = Endpoint {
    type USER_INPUT_TYPES = EXECUTIONS2;
    type HANDLER_TYPES    = EXECUTIONS1
  }
}
