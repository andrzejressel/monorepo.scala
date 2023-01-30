package com.andrzejressel.scalops.ipc.lambda

import com.andrzejressel.scalops.ipc.core.ArrayImplicits.given
import com.andrzejressel.scalops.ipc.core.*
import com.andrzejressel.scalops.lambda.Checker
import zio.*
import zio.schema.Schema.apply
import zio.schema.codec.{BinaryCodec, MessagePackCodec}
import zio.schema.meta.{ExtensibleMetaSchema, MetaSchema}
import zio.schema.{DeriveSchema, Schema}

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

object Lambda {

  type LAMBDA_EXECUTION = Execution[
    "lambda_execution",
    LambdaInput,
    Array[Byte]
  ]

  def createLambdaHandler(classLoader: ClassLoader): executionInput[
    LAMBDA_EXECUTION
  ] => UIO[
    executionOutput[
      LAMBDA_EXECUTION
    ]
  ] =
    (input: LambdaInput) => {
      val schema    = MetaSchema.schema
      val msgSchema = MessagePackCodec.messagePackCodec(schema)

      val inputSchema =
        msgSchema
          .decode(Chunk.fromArray(input.inputSchema))
          .toOption
          .get

      val outputSchema =
        msgSchema
          .decode(Chunk.fromArray(input.outputSchema))
          .toOption
          .get

      val inputCodec = MessagePackCodec
        .messagePackCodec(inputSchema.toSchema)

      val outputCodec = MessagePackCodec
        .messagePackCodec(outputSchema.toSchema)
        .asInstanceOf[BinaryCodec[Any]]

      val realInput =
        inputCodec.decode(Chunk.fromArray(input.input)).toOption.get

      val function =
        deserialize(input.function, classLoader).asInstanceOf[Any => Any]

      val output = function(realInput)

      ZIO.succeed(outputCodec.encode(output).toArray)

    }

  extension [TO_HANDLE <: Tuple, B <: Tuple](
      builder: HandlerBuilder[TO_HANDLE, B]
  ) {
    inline transparent def addLambdaHandler(cl: ClassLoader)(using
        REQUIRES_HANDLER[TO_HANDLE, LAMBDA_EXECUTION]
    ) = {
      builder.handle[LAMBDA_EXECUTION](createLambdaHandler(cl))
    }
  }

  extension (endpoint: Endpoint)(using
      LAMBDA_EXECUTION <:< scala.Tuple.Union[endpoint.USER_INPUT_TYPES]
  )
    inline def runFunction[B](inline f: () => B)(using
        schemaOut: Schema[B]
    ): Task[B] =
      Lambda.runFunction[Unit, B](endpoint, (), (_) => f())

    inline def runFunction[A, B](a: A)(inline f: A => B)(using
        schemaIn: Schema[A],
        schemaOut: Schema[B]
    ): Task[B] =
      Lambda.runFunction[A, B](endpoint, a, f)

  inline private def runFunction[A, B](
      endpoint: Endpoint,
      functionInput: A,
      inline f: A => B
  )(using
      schemaIn: Schema[A],
      schemaOut: Schema[B]
  )(using
      LAMBDA_EXECUTION <:< scala.Tuple.Union[endpoint.USER_INPUT_TYPES]
  ): zio.Task[B] = {
    Checker.check(f)
    val schemaInMeta  = MetaSchema.fromSchema(schemaIn)
    val schemaOutMeta = MetaSchema.fromSchema(schemaOut)

    val mgcMetaSchema = MessagePackCodec.messagePackCodec(MetaSchema.schema)
    val schemaInSerialized  = mgcMetaSchema.encode(schemaInMeta)
    val schemaOutSerialized = mgcMetaSchema.encode(schemaOutMeta)

    val inputSerialized =
      MessagePackCodec.messagePackCodec(schemaIn).encode(functionInput)

    val dto = LambdaInput(
      inputSchema = schemaInSerialized.toArray,
      input = inputSerialized.toArray,
      outputSchema = schemaOutSerialized.toArray,
      function = serialize(f)
    )

    endpoint
      .execute[LAMBDA_EXECUTION](dto)
      .map(arr =>
        MessagePackCodec
          .messagePackCodec(schemaOut)
          .decode(Chunk.fromArray(arr))
          .toOption
          .get
      )
  }

  private def serialize(message: Any): Array[Byte] = {
    val byteArrayOutputStream = ByteArrayOutputStream()
    java.io.ObjectOutputStream(byteArrayOutputStream).writeObject(message)
    byteArrayOutputStream.toByteArray
  }

  private def deserialize(
      serialized: Array[Byte],
      classLoader: ClassLoader
  ): Any = {
    val ois = new CustomClassLoaderObjectInputStream(
      ByteArrayInputStream(serialized),
      classLoader
    )
    ois.readObject()
  }

}
