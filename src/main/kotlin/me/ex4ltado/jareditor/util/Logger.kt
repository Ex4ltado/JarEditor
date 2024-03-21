package me.ex4ltado.jareditor.util

import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

class Logger(name: String) {

    private val impl: Logger = Logger.getLogger(name)

    init {
        impl.level = Level.ALL

        // Get the handlers
        var handlers = impl.handlers

        // If there are no handlers, add one
        if (handlers.isEmpty()) {
            val handler = ConsoleHandler()
            impl.addHandler(handler)
            handlers = impl.handlers
        }

        // Set the level for each handler
        handlers.forEach {
            it.level = Level.ALL
        }
    }

    val isDebugEnabled: Boolean
        get() = impl.isLoggable(Level.FINE)

    val isTraceEnabled: Boolean
        get() = impl.isLoggable(Level.FINE)

    fun debug(s: String) {
        impl.log(Level.FINE, s)
    }

    fun debug(s: String, e: Throwable?) {
        impl.log(Level.FINE, s, e)
    }

    fun debug(s: String, vararg o: Any?) {
        impl.log(Level.FINE, s, o)
    }

    fun info(s: String) {
        impl.log(Level.FINE, s)
    }

    fun error(s: String) {
        impl.log(Level.SEVERE, s)
    }

    fun error(s: String, e: Throwable?) {
        impl.log(Level.SEVERE, s, e)
    }

    fun error(s: String, vararg o: Any?) {
        impl.log(Level.SEVERE, s, o)
    }

    fun warn(s: String) {
        impl.log(Level.WARNING, s)
    }

    fun warn(s: String, e: Throwable?) {
        impl.log(Level.WARNING, s, e)
    }

}
