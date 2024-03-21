package me.ex4ltado.jareditor.editor

import me.ex4ltado.jareditor.CodeBuilder
import me.ex4ltado.jareditor.JeFile
import me.ex4ltado.jareditor.JeMethod
import me.ex4ltado.jareditor.util.Logger
import me.ex4ltado.jareditor.util.LoggerFactory

interface ClassEditor {

    val logger: Logger
        get() = LoggerFactory.getLogger(this::class.java)

    enum class InsertionMode {
        /**
         * Insert the code at the beginning of the source code.
         */
        SOURCE_BEGIN,

        /**
         * Insert the code at the end of the source code.
         */
        SOURCE_END,

        /**
         * Insert the code as a shutdown hook.
         */
        SHUTDOWN_HOOK,

        /**
         * Insert the code to be run in a new thread.
         */
        NEW_THREAD,

        /**
         * Overwrite the existing code with the new code.
         */
        OVERWRITE
    }

    /**
     * Modifies a method with the provided code and insertion mode.
     *
     * @param jar The JAR file to modify.
     * @param code The code to insert.
     * @param method The method to modify.
     * @param mode The mode of insertion.
     */
    fun editMethod(jar: JeFile, code: CodeBuilder, method: JeMethod, mode: InsertionMode)

    /**
     * Injects a class loader into a method.
     *
     * @param jar The JAR file to modify.
     * @param method The method to inject the class loader into.
     * @param classPath The name of the class to load.
     * @param classBytecode The byte array representation of the class to load.
     * @param methodToInvoke The name of the method to invoke on the loaded class.
     * @param mode The insertion mode for the injected code.
     */
    fun injectClassLoader(
        jar: JeFile,
        method: JeMethod,
        classPath: String,
        classBytecode: ByteArray,
        methodToInvoke: String,
        mode: InsertionMode
    )

    /**
     * Injects shellcode into a method.
     *
     * @param jar The JAR file to modify.
     * @param method The method to inject the shellcode into.
     * @param payload The shellcode to inject.
     * @param mode The insertion mode for the injected code.
     */
    fun injectShellcode(jar: JeFile, method: JeMethod, payload: String, mode: InsertionMode)

    /**
     * Nullifies a method. This is done by replacing the method body with a return zero or null statement.
     *
     * @param jar The JAR file to modify.
     * @param method The method to nullify.
     */
    fun nullifyMethod(jar: JeFile, method: JeMethod)

}