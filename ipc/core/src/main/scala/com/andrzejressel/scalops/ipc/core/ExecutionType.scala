package com.andrzejressel.scalops.ipc.core

import scodec.codecs.bool
import scodec.{Codec, Iso}

private enum ExecutionType derives Codec {
  case NewExecution, ReturningExecution
}
