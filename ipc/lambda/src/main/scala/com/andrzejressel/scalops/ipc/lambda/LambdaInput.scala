package com.andrzejressel.scalops.ipc.lambda

import com.andrzejressel.scalops.ipc.core.ArrayImplicits.given
import zio.schema.*

case class LambdaInput(
    val inputSchema: Array[Byte],
    val input: Array[Byte],
    val outputSchema: Array[Byte],
    val function: Array[Byte]
)
object LambdaInput {
  given Schema[LambdaInput] = DeriveSchema.gen[LambdaInput]
}
