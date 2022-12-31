package com.andrzejressel.scalops.ipc.core

import java.io.{PipedInputStream, PipedOutputStream}

/** PipedOutputStream with fixed performance based on
  *
  * https://stackoverflow.com/questions/28617175/did-i-find-a-bug-in-java-io-pipedinputstream#comment78487803_28617908
  */
class FlushingPipedOutputStream(snk: PipedInputStream)
    extends PipedOutputStream(snk) {

  override def write(b: Int): Unit = {
    super.write(b)
    flush()
  }

  override def write(b: Array[Byte], off: Int, len: Int): Unit = {
    super.write(b, off, len)
    flush()
  }

}
