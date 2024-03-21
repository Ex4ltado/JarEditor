package me.ex4ltado.jareditor.util

import java.security.SecureRandom

fun String.withSuffix(suffix: String): String =
    if (this.endsWith(suffix)) this else this + suffix

fun String.toJavaName(): String =
    this.replace('.', '/').withSuffix(".class")

fun String.toJvmName(): String =
    this.replace('/', '.').removeSuffix(".class")

fun String.countChar(char: Char): Int =
    this.count { it == char }

fun String.obfuscate(): ByteArray {
    val r = SecureRandom()
    val b: ByteArray = this.toByteArray()
    val obfBytes = ByteArray(b.size)
    for (i in b.indices) {
        val f: Int = r.nextInt(24) + 1
        var t: Int = r.nextInt()
        t = t and (0xff shl f).inv() or (b[i].toInt() shl f)
        obfBytes[i] = (t shr f).toByte()
    }
    return obfBytes
}

fun String.indexOfAfterCounter(char: Char, count: Int): Int {
    var counter = 0
    for (i in this.indices) {
        if (this[i] == char) {
            counter++
            if (counter == count) {
                return i
            }
        }
    }
    return -1
}