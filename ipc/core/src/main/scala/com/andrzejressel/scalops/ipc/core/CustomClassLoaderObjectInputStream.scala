package com.andrzejressel.scalops.ipc.core

import java.io.{InputStream, ObjectInputStream, ObjectStreamClass}
import scala.util.Try

import collection.immutable

class CustomClassLoaderObjectInputStream(
    in: InputStream,
    classLoader: ClassLoader
) extends ObjectInputStream(in) {

  private val primClasses = Map(
    "boolean" -> classOf[Boolean],
    "byte"    -> classOf[Byte],
    "char"    -> classOf[Char],
    "short"   -> classOf[Short],
    "int"     -> classOf[Int],
    "long"    -> classOf[Long],
    "float"   -> classOf[Float],
    "double"  -> classOf[Double],
    "void"    -> classOf[Unit]
  )

  override def resolveClass(desc: ObjectStreamClass): Class[?] = {
    val name = desc.getName
    println(s"Resolving ${name}")
    Try(Class.forName(name, false, classLoader)).recoverWith {
      case ex: ClassNotFoundException =>
        primClasses.get(name).toRight(ex).toTry
    }.get
  }
}
