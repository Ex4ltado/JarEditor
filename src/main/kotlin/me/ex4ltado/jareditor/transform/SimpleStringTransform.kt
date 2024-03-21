package me.ex4ltado.jareditor.transform

import java.security.SecureRandom

class SimpleStringTransform : Transform<String, String>() {

    override fun transform(obj: String): String {
        if (obj.isBlank())
            throw IllegalArgumentException("String cannot be blank")
        val r = SecureRandom()
        val b: ByteArray = obj.toByteArray()
        val sb = StringBuilder()
        sb.append("new String(new byte[]{")
        for (i in b.indices) {
            val f: Int = r.nextInt(24) + 1
            var t: Int = r.nextInt()
            t = t and (0xff shl f).inv() or (b[i].toInt() shl f)
            sb.append("(byte) ($t >>> $f),")
        }
        sb.deleteCharAt(sb.length - 1) // remove last comma
        sb.append("})")
        return sb.toString()
    }

}