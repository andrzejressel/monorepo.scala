package com.andrzejressel.scalops.ipc.core

import zio.schema.Schema

import scala.reflect.ClassTag

type Execution[ID <: String & Singleton, INPUT, OUTPUT] = (ID, INPUT, OUTPUT)

type ExecutionToTuple[x <: Execution[?, ?, ?]] <: Tuple = x match {
  case Execution[id, input, output] => (id, input, output)
}

type executionInput[x <: Any] = x match {
  case Execution[?, input, ?] => input
}

type executionOutput[x <: Any] = x match {
  case Execution[?, ?, output] => output
}

type executionId[x <: Any] = x match {
  case Execution[id, ?, ?] => id
}

type API_DECLARATION[Executions1 <: Tuple, Executions2 <: Tuple]

type Executions1Get[x <: Any] <: Tuple = x match {
  case API_DECLARATION[e1, ?] => e1
}
type Executions2Get[x <: Any] <: Tuple = x match {
  case API_DECLARATION[?, e2] => e2
}

case class ExecutionImpl(
    id: String,
    in: zio.schema.Schema[?],
    out: zio.schema.Schema[?]
)
