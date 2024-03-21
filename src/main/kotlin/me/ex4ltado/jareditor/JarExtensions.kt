package me.ex4ltado.jareditor

import javassist.CtMethod
import me.ex4ltado.jareditor.util.toJavaName
import me.ex4ltado.jareditor.util.toJvmName

typealias ClassPath = String

typealias Bytecode = ByteArray

typealias JeClass = Pair<ClassPath, Bytecode>

typealias JeMethod = CtMethod

fun JeClass.name() = first

fun JeClass.jvmClassName() = first.toJvmName()

fun JeClass.javaClassName() = first.toJavaName()

fun JeClass.className() = first.substringAfterLast("/")

fun JeClass.packageName() = first.substringBeforeLast("/")

fun JeClass.toBytecode() = second
