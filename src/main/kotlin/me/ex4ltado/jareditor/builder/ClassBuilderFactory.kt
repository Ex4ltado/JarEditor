package me.ex4ltado.jareditor.builder

import me.ex4ltado.jareditor.ClassManager
import me.ex4ltado.jareditor.InlineCode

object ClassBuilderFactory {

    /**
     * Creates an [ClassBuilder] that implements the [Runnable] interface.
     *
     * @param className
     * @param packagePath
     * @param body
     * @return [ClassBuilder]
     */
    inline fun runnableClass(
        classManager: ClassManager,
        className: String,
        packagePath: String,
        body: InlineCode
    ): ClassBuilder {
        val runnable = ClassBuilder(classManager, className, packagePath, true)
            .addInterface(Runnable::class.qualifiedName!!) // Implements Runnable interface
            .addMethod("run",
                body = {
                    body()
                })
            .create()
        return runnable
    }

    /**
     * Creates an [ClassBuilder] with one method.
     * Can be used to call the method by reflection or calling the method directly.
     *
     * @param className
     * @param packagePath
     * @param body
     * @return [ClassBuilder]
     */
    inline fun wrapperClass(
        classManager: ClassManager,
        className: String,
        methodName: String,
        packagePath: String,
        body: InlineCode,
    ): ClassBuilder {
        val wrapper = ClassBuilder(classManager, className, packagePath, true)
            .addMethod(methodName,
                body = {
                    body()
                })
            .create()
        return wrapper
    }

}