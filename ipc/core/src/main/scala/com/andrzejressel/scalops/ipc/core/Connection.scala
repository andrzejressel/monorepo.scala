package com.andrzejressel.scalops.ipc.core

import scodec.bits.{*, given}
import scodec.codecs.{*, given}
import scodec.{*, given}
import zio.*
import zio.concurrent.ReentrantLock

import java.io.{InputStream, OutputStream}
import java.nio.ByteBuffer

class Connection(
    private val in: InputStream,
    private val out: OutputStream,
    private val writeLock: ReentrantLock
) {

  def getNext: Task[Array[Byte]] =
    ZIO.attemptBlockingInterrupt(_getNextSync())

  private def _getNextSync() = {
    val bb = ByteBuffer.allocate(4)

    val sizeArr = in.readNBytes(4)
    if sizeArr.length != 4 then {
      throw IllegalStateException("EOF")
    }
    bb.put(sizeArr)
    bb.flip()
    val size = bb.getInt
    val arr  = in.readNBytes(size)
    if arr.length != size then {
      throw IllegalStateException("EOF")
    }
    arr
  }

  def send(message: Seq[Byte]): Task[Unit] =
    ZIO.scoped(writeLock.withLock *> _send(message))

  private def _send(message: Seq[Byte]) =
    ZIO.attemptBlockingInterrupt(_sendSync(message))

  private def _sendSync(message: Seq[Byte]): Unit = {
    val bb = ByteBuffer.allocate(4)
    bb.putInt(message.length)
    out.write(bb.array())
    out.write(message.toArray)
  }
}

object Connection {
  def apply[P](in: InputStream, out: OutputStream): URIO[Scope, Connection] = {
    for {
      l <- ReentrantLock.make()
      connection = new Connection(in, out, l)
    } yield connection
  }
}
