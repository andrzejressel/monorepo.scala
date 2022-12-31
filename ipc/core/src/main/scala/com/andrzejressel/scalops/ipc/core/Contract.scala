package com.andrzejressel.scalops.ipc.core

import scodec.Codec.*
import scodec.*
import scodec.bits.*
import scodec.codecs.*
import zio.Chunk.ByteArray
import zio.concurrent.ConcurrentMap
import zio.schema.Schema
import zio.schema.codec.{BinaryCodec, JsonCodec, MessagePackCodec}
import zio.{Chunk, IO, Promise, Task, UIO, ZIO}

import java.util.UUID
import scala.Tuple.{
  Elem as TElem,
  Filter as TFilter,
  Map as TMap,
  Size as TSize,
  Union as TUnion,
  Zip as TZip
}
import scala.annotation.*
import scala.collection.immutable
import scala.compiletime.*
import scala.deriving.*
import scala.reflect.{ClassTag, Typeable}
import scala.util.Random

import ExecutionType.*
import language.experimental.erasedDefinitions

trait SchemaPair {
  type INPUT <: Any
  type OUTPUT <: Any

  val inputSchema: zio.schema.Schema[INPUT]
  val outputSchema: zio.schema.Schema[OUTPUT]
}

trait Endpoint {
  type USER_INPUT_TYPES <: Tuple
  type HANDLER_TYPES <: Tuple

  protected val connection: Connection
  protected val handlers: Handlers[HANDLER_TYPES]
  protected val schemas: immutable.Map[String, SchemaPair]
  protected val executionMap: ConcurrentMap[UUID, Promise[Nothing, Any]]

  private val codec =
    Codec[ExecutionType] :: uuid :: utf8_32 :: Codec[ByteVector]

  inline def execute[A <: Execution[?, ?, ?]](
      argument: TElem[ExecutionToTuple[A], 1]
  )(using
      @unused t: ExecutionToTuple[A] <:< (String & Singleton, ?, ?),
      @unused @implicitNotFound(
        "Invalid execution type"
      ) p: A <:< TUnion[USER_INPUT_TYPES]
  ): Task[TElem[ExecutionToTuple[A], 2]] = {
    val id = inline erasedValue[A] match
      case _: Execution[id, ?, ?] =>
        constValue[id].asInstanceOf[String]

    execute0(id, argument)
  }

  def looper(): Task[Nothing] = {
    ZIO.scoped(
      (for {
        arr <- connection.getNext
        _ <- handle(arr)
          .tapErrorCause(c =>
            ZIO.succeed(println(s"Looper error: ${c.toString()}"))
          )
          .forkScoped
      } yield ()).forever
    )
  }

  private def execute0[INPUT, OUTPUT](
      id: String,
      obj: INPUT
  ): Task[OUTPUT] = for {
    executionId <- zio.Random.nextUUID
    // _ <- zio.Console.printLine(
    // s"Executing [$id] with uuid [${executionId}] with [$obj]"
    // )
    _ <- zio.Console.printLine(
      s"Executing [$id] with uuid [${executionId}]"
    )
    promise    <- Promise.make[Nothing, Any]
    serialized <- ZIO.attempt(serialize1(id, obj, executionId))
    _          <- connection.send(serialized.bytes.toSeq)
    _          <- executionMap.put(executionId, promise)
    _          <- zio.Console.printLine(s"Awaiting [$executionId]")
    value      <- promise.await.map(_.asInstanceOf[OUTPUT])
    _          <- executionMap.remove(executionId)
    // _ <- zio.Console.printLine(s"Resolved [$executionId] with [$value]")
    _ <- zio.Console.printLine(s"Resolved [$executionId]")
  } yield value

  private def serialize1(id: String, obj: Any, executionId: UUID): BitVector = {
    val schema = schemas(id)
    val serialized =
      MessagePackCodec
        .messagePackCodec(schema.inputSchema)
        .encode(obj.asInstanceOf[schema.INPUT])

    codec
      .encode(NewExecution, executionId, id, ByteVector(serialized.toArray))
      .require
  }

  private def serialize2(id: String, obj: Any, executionId: UUID): BitVector = {
    val schema = schemas(id)
    val serialized =
      MessagePackCodec
        .messagePackCodec(schema.outputSchema)
        .encode(obj.asInstanceOf[schema.OUTPUT])

    codec
      .encode(
        ReturningExecution,
        executionId,
        id,
        ByteVector(serialized.toArray)
      )
      .require
  }

  private def deserialize1(id: String, data: ByteVector): IO[String, Any] = {
    val schema = schemas(id)

    ZIO
      .fromEither(
        MessagePackCodec
          .messagePackCodec(schema.inputSchema)
          .decode(Chunk.fromArray(data.toArray))
      )
      .mapError(_.getMessage())

  }

  private def deserialize2(id: String, data: ByteVector): IO[String, Any] = {
    val schema = schemas(id)

    ZIO
      .fromEither(
        MessagePackCodec
          .messagePackCodec(schema.outputSchema)
          .decode(Chunk.fromArray(data.toArray))
      )
      .mapError(_.getMessage())

  }

  private def handle(
      data: Array[Byte]
  ): IO[Any, Unit] = {

    for {
      (executionType, executionUUID, executionId, data) <- ZIO
        .fromEither(codec.decode(BitVector(data)).toEither)
        .map(_.value)

      // _ <- zio.Console.printLine(
      //   s"Received executionType: [${executionType}] executionUUID: [${executionUUID}] executionId: [$executionId] json: [$json]"
      // )
      _ <- zio.Console.printLine(
        s"Received executionType: [${executionType}] executionUUID: [${executionUUID}] executionId: [$executionId]"
      )
      _ <- executionType match
        case NewExecution =>
          deserialize1(executionId, data).flatMap(value =>
            handleNewExecution(executionUUID, executionId, value)
          )
        case ReturningExecution =>
          deserialize2(executionId, data).flatMap(value =>
            handleReturnValue(executionUUID, value)
          )
    } yield ()
  }

  private def handleReturnValue(
      uuid: UUID,
      value: Any
  ) = {
    for {
      promise <- executionMap.get(uuid).map(_.get)
      _       <- promise.succeed(value)
    } yield ()
  }

  private def handleNewExecution(
      executionId: UUID,
      id: String,
      value: Any
  ) = {
    val handler = handlers.map(id)
    for {
      result     <- handler.function.asInstanceOf[Any => Task[Any]](value)
      serialized <- ZIO.attempt(serialize2(id, result, executionId))
      _          <- connection.send(serialized.bytes.toSeq)
    } yield result
  }

}

transparent inline def createEndpoint1[API](
    handlers: Handlers[Executions1Get[API]],
    connection: Connection
) = {
  createEndpoint[Executions2Get[API], Executions1Get[API]](handlers, connection)
}

transparent inline def createEndpoint2[API](
    handlers: Handlers[Executions2Get[API]],
    connection: Connection
) = {
  createEndpoint[Executions1Get[API], Executions2Get[API]](handlers, connection)
}

type Filter2[Obj, Tup <: Tuple, P[_, _] <: Boolean] <: Tuple = Tup match {
  case EmptyTuple => EmptyTuple
  case h *: t =>
    P[Obj, h] match {
      case true  => h *: Filter2[Obj, t, P]
      case false => Filter2[Obj, t, P]
    }
}

type HadId[ID, x] <: Boolean = x match {
  case Execution[ID, ?, ?] => true
  case Execution[?, ?, ?]  => false
}

type NotHadId[ID, x] <: Boolean = x match {
  case Execution[ID, ?, ?] => false
  case Execution[?, ?, ?]  => true
}

case class HandlerSchemas(
    function: (?) => zio.UIO[?],
    inputSchema: zio.schema.Schema[?],
    outputSchema: zio.schema.Schema[?]
)

class Handlers[EXECUTIONS <: Tuple](
    val map: scala.collection.immutable.Map[String, HandlerSchemas]
)

val EMPTY_HANDLER = new Handlers[EmptyTuple](Map())

type REQUIRES_HANDLER[TO_HANDLE <: Tuple, E] =
  TSize[Filter2[executionId[E], TO_HANDLE, HadId]] =:= 1

class HandlerBuilder[TO_HANDLE <: Tuple, ALL_EXECUTIONS <: Tuple] private (
    private val map: scala.collection.immutable.Map[String, HandlerSchemas]
) {

  // We cannot use private constructor in inline, but we can use private methods ...
  private def create[NEW_TYPE <: Tuple](
      newMap: immutable.Map[String, HandlerSchemas]
  ) = HandlerBuilder[NEW_TYPE, ALL_EXECUTIONS](newMap)

  type NEW_HANDLE_TYPE[E <: Execution[?, ?, ?]] =
    Filter2[executionId[E], TO_HANDLE, NotHadId]

  inline def handle[E <: Execution[?, ?, ?]](using
      REQUIRES_HANDLER[TO_HANDLE, E]
  )(
      a: executionInput[E] => zio.UIO[executionOutput[E]]
  )(using
      inputSchema: zio.schema.Schema[executionInput[E]],
      outputSchema: zio.schema.Schema[executionOutput[E]]
  ): HandlerBuilder[
    NEW_HANDLE_TYPE[E],
    ALL_EXECUTIONS
  ] = {
    val id = constValue[executionId[E]].asInstanceOf[String]
    create(map + (id -> HandlerSchemas(a, inputSchema, outputSchema)))
  }

  def build()(using
      @unused u1: TO_HANDLE =:= EmptyTuple
  ): Handlers[ALL_EXECUTIONS] =
    new Handlers(map)

}

object HandlerBuilder {
  def create[T <: Tuple] = new HandlerBuilder[T, T](Map())
}

private transparent inline def createEndpoint[
    USER <: Tuple,
    CONNECTION <: Tuple
](
    channelHandler: Handlers[CONNECTION],
    connectionArg: Connection
) = {

  val map = createSchemaMap[scala.Tuple.Concat[USER, CONNECTION]]

  zio.concurrent.ConcurrentMap
    .empty[UUID, Promise[Nothing, Any]]
    .map(concurrentMap =>
      new Endpoint {
        override type USER_INPUT_TYPES = USER
        override type HANDLER_TYPES    = CONNECTION
        override protected val connection: Connection = connectionArg
        override val handlers: Handlers[CONNECTION]   = channelHandler
        override val schemas: Map[String, SchemaPair] = map
        override protected val executionMap
            : ConcurrentMap[UUID, Promise[Nothing, Any]] = concurrentMap
      }
    )

}

private inline def createSchemaMap[T <: Tuple]
    : immutable.Map[String, SchemaPair] = {
  inline erasedValue[T] match
    case _: EmptyTuple => Map()
    // noinspection ScalaUnnecessaryParentheses
    case _: (Execution[id, input, output] *: ts) =>
      // val inputSchema  =
      // val outputSchema =

      createSchemaMap[ts] + (constValue[id] ->
        new SchemaPair {
          type INPUT  = input
          type OUTPUT = output
          override val inputSchema =
            summonInline[zio.schema.Schema[input]].asInstanceOf[Schema[INPUT]]
          override val outputSchema =
            summonInline[zio.schema.Schema[output]].asInstanceOf[Schema[OUTPUT]]
        })
}
