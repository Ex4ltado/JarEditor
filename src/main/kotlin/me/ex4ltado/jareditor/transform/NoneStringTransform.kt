package me.ex4ltado.jareditor.transform

class NoneStringTransform : Transform<String, String>() {

    override fun transform(obj: String): String {
        return obj
    }

}