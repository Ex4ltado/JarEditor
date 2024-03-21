package me.ex4ltado.jareditor.util

import javassist.CtClass
import me.ex4ltado.jareditor.ClassManager
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

fun CtClass.toByteArray(): ByteArray {
    val byteArray = ByteArrayOutputStream()
    val out = DataOutputStream(byteArray)
    out.use { o ->
        this.toBytecode(o)
    }
    return byteArray.toByteArray()
}


fun CtClass.toClassBuilder(
    classManager: ClassManager,
    block: me.ex4ltado.jareditor.builder.ClassBuilder.() -> Unit
): me.ex4ltado.jareditor.builder.ClassBuilder {
    if (this.isFrozen)
        this.defrost()
    val builder =
        me.ex4ltado.jareditor.builder.ClassBuilder(classManager, this.simpleName, this.packageName ?: "", false)
    builder.block()
    if (!builder.created)
        builder.create()
    return builder
}
