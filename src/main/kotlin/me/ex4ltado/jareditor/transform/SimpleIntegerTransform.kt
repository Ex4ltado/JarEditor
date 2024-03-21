package me.ex4ltado.jareditor.transform

import kotlin.random.Random

class SimpleIntegerTransform : Transform<Int, String>() {

    private fun toHexCharArray(int: Int): CharArray {
        val hex = int.toString(16)
        return hex.toCharArray()
    }

    private fun parseInt(chars: CharArray): String {
        return "Integer.parseInt(new String(new byte[] {${chars.joinToString(",") { "(byte)'$it'" }}}), 16)"
    }

    // obfuscate an integer transforming it to a hex string and then to a char array
    override fun transform(obj: Int): String {
        val chars = toHexCharArray(obj)
        val rInt = toHexCharArray(Random.nextInt())
        /*val steps = Random.nextInt(2, 10)
        var s = "${parseInt(chars)}"
        for (i in 0 until steps) {
            s = "Integer.parseInt(\"$s\", 16)"
        }*/
        return parseInt(chars)
    }

}