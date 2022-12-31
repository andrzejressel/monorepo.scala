package com.andrzejressel.scalops.ipc.core

import zio.Chunk
import zio.schema.Schema

import scala.reflect.ClassTag

object ArrayImplicits {
  given [A](using c: Schema[Chunk[A]])(using ClassTag[A]): Schema[Array[A]] =
    c.transform(_.toArray, Chunk.fromArray)
}
