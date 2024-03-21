package me.ex4ltado.jareditor.builder

import javassist.*
import me.ex4ltado.jareditor.ClassManager
import me.ex4ltado.jareditor.InlineCode
import me.ex4ltado.jareditor.JeMethod
import me.ex4ltado.jareditor.codeBuilder
import me.ex4ltado.jareditor.util.LoggerFactory
import me.ex4ltado.jareditor.util.withSuffix
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

/**
 * ClassBuilder is a utility class for dynamically creating new classes using Javassist.
 *
 * @property classManager The ClassPool object that will be used to create the class.
 * @property className The name of the class to be created.
 * @property packagePath The package path where the class will be created.
 * @property override Whether to override the class if it already exists.
 */
class ClassBuilder(
    private val classManager: ClassManager,
    val className: String,
    val packagePath: String,
    override: Boolean
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    private val currentPool = classManager.classPool

    // The CtClass object that represents the class being created.
    private val ctClass: CtClass

    // A flag indicating whether the class has been created.
    var created = false
        private set

    init {
        val classPath = if (packagePath.isNotBlank()) {
            packagePath.replace("/", ".").withSuffix(".") + className
        } else {
            className
        }
        ctClass = if (override) {
            currentPool.makeClass(classPath)
        } else {
            try {
                currentPool.get(classPath)
            } catch (e: NotFoundException) {
                logger.info("Class $classPath not found, creating new one")
                currentPool.makeClass(classPath)
            }
        }
        if (ctClass.isFrozen) {
            ctClass.defrost()
        }
    }

    /**
     * Adds a superclass to the class being created.
     *
     * @param superClass The name of the superclass.
     * @throws IllegalStateException If the class has already been created.
     * @return The ClassBuilder instance.
     */
    @Throws(IllegalStateException::class)
    fun addSuperClass(superClass: String): ClassBuilder {
        if (created)
            throw IllegalStateException("Class already created")
        ctClass.superclass = currentPool.get(superClass)
        return this
    }

    /**
     * Adds a constructor to the class being created.
     *
     * @param src The source code of the constructor.
     * @return The ClassBuilder instance.
     */
    fun addConstructor(src: String): ClassBuilder {
        ctClass.addConstructor(CtNewConstructor.make(src, ctClass))
        return this
    }

    /**
     * Adds an interface to the class being created.
     *
     * @param className The name of the interface.
     * @throws IllegalStateException If the class has already been created.
     * @return The ClassBuilder instance.
     */
    @Throws(IllegalStateException::class)
    fun addInterface(className: String): ClassBuilder {
        if (created)
            throw IllegalStateException("Class already created")
        ctClass.addInterface(currentPool.get(className))
        return this
    }

    /**
     * Adds a method to the class being created.
     *
     * @param method The JeMethod object representing the method.
     * @throws IllegalStateException If the class has already been created.
     * @return The ClassBuilder instance.
     */
    @Throws(IllegalStateException::class)
    private fun addMethod(method: JeMethod): ClassBuilder {
        if (created)
            throw IllegalStateException("Class already created")
        /*if (ctClass.methods.firstOrNull { it.name == method.name } != null)
            throw IllegalStateException("Method already exists")*/
        ctClass.addMethod(method)
        return this
    }

    /**
     * Adds a method to the class being created.
     *
     * @param src The source code of the method.
     * @return The ClassBuilder instance.
     */
    fun addMethod(src: String): ClassBuilder {
        addMethod(CtNewMethod.make(src, ctClass))
        return this
    }

    /**
     * Adds a method to the class being created.
     *
     * @param methodName The name of the method.
     * @param accessLevel The visibility of the method (public, private, etc.).
     * @param static Whether the method is static.
     * @param body The body of the method.
     * @return The ClassBuilder instance.
     */
    inline fun addMethod(
        methodName: String,
        accessLevel: AccessLevel = AccessLevel.PUBLIC,
        static: Boolean = false,
        body: InlineCode,
    ): ClassBuilder {
        val methodSignature = "${accessLevel.modifier} ${if (static) "static" else ""} void $methodName()"
        val code = codeBuilder {
            line("$methodSignature {")
            this.body()
            line("}")
        }
        return addMethod(code.toString())
    }

    /**
     * Adds a method to the class being created.
     *
     * @param methodName The name of the method.
     * @param accessLevel The visibility of the method (public, private, etc.).
     * @param static Whether the method is static.
     * @param body The body of the method.
     * @param returnType The return type of the method.
     * @param parameters The parameters of the method.
     * @return The ClassBuilder instance.
     */
    inline fun addMethod(
        methodName: String,
        accessLevel: AccessLevel = AccessLevel.PUBLIC,
        static: Boolean = false,
        body: InlineCode,
        returnType: Class<*> = Void.TYPE,
        vararg parameters: Parameter
    ): ClassBuilder {
        val methodSignature = "${accessLevel.modifier} ${if (static) "static" else ""} ${returnType.name} $methodName"
        val methodParams = parameters.joinToString(",") { it.type.name + " " + it.name }
        val code = codeBuilder {
            line("$methodSignature($methodParams) {")
            this.body()
            line("}")
        }
        return addMethod(code.toString())
    }

    fun addField(src: String): ClassBuilder {
        ctClass.addField(CtField.make(src, ctClass))
        return this
    }

    /**
     * Creates the class.
     *
     * @throws CannotCompileException If the class cannot be compiled.
     * @throws IllegalStateException If the class has already been created.
     * @return The ClassBuilder instance.
     */
    @Throws(CannotCompileException::class, IllegalStateException::class)
    fun create(): ClassBuilder {
        if (created)
            throw IllegalStateException("Class already created")
        ctClass.toClass()
        created = true
        return this
    }

    /**
     * Returns the CtClass object representing the class.
     *
     * @throws IllegalStateException If the class has not been created.
     * @return The CtClass object.
     */
    @Throws(IllegalStateException::class)
    fun toCtClass(): CtClass {
        if (!created)
            throw IllegalStateException("Class not created")
        return ctClass
    }

    /**
     * Returns the bytecode of the class.
     *
     * @throws IllegalStateException If the class has not been created.
     * @return The bytecode of the class.
     */
    @Throws(IllegalStateException::class)
    fun toBytecode(): ByteArray {
        if (!created)
            throw IllegalStateException("Class not created")
        val outputStream = ByteArrayOutputStream()
        val out = DataOutputStream(outputStream)
        out.use { o ->
            ctClass.toBytecode(o)
        }
        return outputStream.toByteArray()
    }

    /**
     * Returns the class path of the class.
     *
     * @return The class path.
     */
    fun classPath(): String {
        return packagePath.replace("/", ".") + className
    }

    /**
     * Returns the methods declared in the class.
     *
     * @return An array of JeMethod objects representing the methods.
     */
    fun declaredMethods(): Array<JeMethod> {
        return ctClass.declaredMethods
    }

}