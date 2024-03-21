package me.ex4ltado.jareditor.util

object LoggerFactory {

    fun getLogger(clazz: Class<*>): Logger {
        return Logger(clazz.name)
    }

    fun getLogger(name: String): Logger {
        return Logger(name)
    }

}
