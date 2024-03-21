package me.ex4ltado.jareditor

import javassist.ClassPool
import javassist.CtClass
import me.ex4ltado.jareditor.util.LoggerFactory
import me.ex4ltado.jareditor.util.toJvmName
import java.nio.file.Path

/**
 * Manages a pool of classes from Jar files.
 *
 * @property classPool The ClassPool to use for creating and modifying classes.
 */
class ClassManager(val classPool: ClassPool) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * A Map storing the Jar files and their corresponding classes.
     */
    private val classesMap: HashMap<JeFile, MutableList<CtClass>> = HashMap()

    /**
     * Adds a Jar file to the pool.
     *
     * @param jeFile The Jar file to add.
     * @throws IllegalStateException If the Jar file has not been read or is already in the pool.
     */
    fun putJar(jeFile: JeFile) {
        if (!jeFile.read)
            throw IllegalStateException("The jar file must be readed before add to the pool")
        if (classesMap.containsKey(jeFile))
            throw IllegalStateException("The jar file is already in the pool")
        classesMap[jeFile] = mutableListOf()
    }

    /**
     * Makes a class and adds it to the Javassist pool.
     *
     * @param jeFile The Jar file containing the class.
     * @param bytecode The bytecode of the class to add.
     * @throws IllegalStateException If the Jar file is not in the pool.
     */
    fun makeClass(jeFile: JeFile, bytecode: ByteArray): CtClass {
        if (!classesMap.containsKey(jeFile))
            throw IllegalStateException("The jar file is not in the pool")
        val classes = classesMap[jeFile]!!
        val ctClass = classPool.makeClassIfNew(bytecode.inputStream())!!
        logger.debug("Class ${ctClass.name} added")
        if (!classes.contains(ctClass)) classes.add(ctClass)
        return ctClass
    }

    /**
     * Get a Jar file from the pool.
     *
     * @param jeFile The Jar file to get.
     */
    fun getJar(jeFile: JeFile): JeFile? =
        classesMap.keys.firstOrNull { it == jeFile }

    /**
     * Get a Jar file from the pool by name.
     *
     * @param jarName The name of the Jar file to get.
     */
    fun getJar(jarName: String): JeFile? =
        classesMap.keys.firstOrNull { it.jarName == jarName }

    /**
     * Returns a Javassist class from the pool.
     *
     * @param jeFile The Jar file containing the class.
     * @param className The class to return.
     * @return The Javassist class.
     * @throws IllegalStateException If the Jar file is not in the pool.
     */
    fun getClass(jeFile: JeFile, className: String): CtClass? {
        if (!classesMap.containsKey(jeFile))
            throw IllegalStateException("The jar file is not in the pool")
        val ctClass = classesMap[jeFile]!!.firstOrNull { it.name == className }
        if (ctClass == null) logger.warn("$className not found on ${jeFile.jarName}")
        return ctClass
    }

    /**
     * Returns a Javassist class from the pool.
     *
     * @param className The class to return.
     * @return The Javassist class.
     */
    fun getClass(className: String): CtClass? =
        classPool[className]

    /**
     * Returns a Javassist method from the pool.
     *
     * @param jeFile The Jar file containing the class.
     * @param className The class containing the method.
     * @param methodName The method to return.
     * @param signature The signature of the method to return.
     * @return The Javassist method.
     * @throws IllegalStateException If the Jar file is not in the pool.
     */
    fun getMethod(jeFile: JeFile, className: String, methodName: String, signature: String): JeMethod? {
        if (!classesMap.containsKey(jeFile))
            throw IllegalStateException("The jar file is not in the pool")
        return classesMap[jeFile]!!.firstOrNull { it.name == className.toJvmName() }?.methods?.firstOrNull { it.name == methodName && it.signature == signature }
    }

    /**
     * Returns a Javassist method from the pool.
     *
     * @param jeClass The class containing the method.
     * @param name The method to return.
     * @param signature The signature of the method to return.
     * @return The Javassist method.
     */
    fun getMethod(jeClass: JeClass, name: String, signature: String): JeMethod? =
        classPool[jeClass.jvmClassName()]!!.methods.firstOrNull { it.name == name && it.signature == signature }

    /**
     * Returns a Javassist method from the pool.
     *
     * @param jeClass The class containing the method.
     * @param name The method to return.
     * @return The Javassist method.
     */
    fun getMethod(jeClass: JeClass, name: String): JeMethod? =
        classPool[jeClass.jvmClassName()]!!.methods.firstOrNull { it.name == name }

    /**
     * Returns a Javassist method from the pool.
     *
     * @param className The class containing the method.
     * @param methodName The method to return.
     * @param signature The signature of the method to return.
     * @return The Javassist method.
     */
    fun getMethod(className: String, methodName: String, signature: String): JeMethod? =
        classPool[className.toJvmName()]!!.methods.first { it.name == methodName && it.signature == signature }

    /**
     * Returns a list of the Jar files in the pool.
     *
     * @return A list of the Jar files in the pool.
     */
    fun getClassesList() = classesMap.toList()

    /**
     * Returns a list of the classes in the pool for a specific Jar file.
     *
     * @param jeFile The Jar file to get the classes for.
     * @return A list of the classes in the pool for the specified Jar file.
     */
    fun getClassesFromJeFile(jeFile: JeFile): List<CtClass>? =
        classesMap[jeFile]?.toList()

    /**
     * Returns a list of the classes in the pool for a specific Jar file.
     *
     * @param jeFile The Jar file to get the classes for.
     * @return A list of the classes in the pool for the specified Jar file.
     */
    fun getClassesFromJarFile(jeFile: JeFile) = classesMap[jeFile]!!

    /**
     * Removes a Jar file from the pool.
     *
     * @param jeFile The Jar file to remove.
     * @throws IllegalStateException If the Jar file is not in the pool.
     */
    fun removeJar(jeFile: JeFile) {
        if (!classesMap.containsKey(jeFile))
            throw IllegalStateException("The jar file is not in the pool")
        detachJeFile(jeFile)
        classesMap.remove(jeFile)
    }

    /**
     * Checks if the pool is empty.
     *
     * @return True if the pool is empty, false otherwise.
     */
    fun isEmpty() = classesMap.isEmpty()

    /**
     * Detaches a Jar file from the pool.
     *
     * @param jeFile The Jar file to detach.
     */
    private fun detachJeFile(jeFile: JeFile) {
        classesMap[jeFile]!!.forEach {
            it.detach()
            logger.debug("Class ${it.name} detached")
        }
    }

    /**
     * Clears the pool.
     */
    fun clearPool() {
        classesMap.map { it.key }.forEach(::detachJeFile)
        classesMap.clear()
    }

}

/**
 * Executes a function in the context of a ClassManager and a JeFile.
 *
 * @param classManager The ClassManager to use.
 * @param jeFile The JeFile to use.
 * @param outputPath The path to write the output to.
 * @param function The function to execute.
 */
inline fun scope(
    classManager: ClassManager,
    jeFile: JeFile,
    outputPath: Path? = null,
    function: ClassManager.() -> Unit
) {
    classManager.putJar(jeFile)
    classManager.function()
    if (outputPath != null) {
        val output = JeFileOutput(jeFile)
        output.write(outputPath, classManager.getClassesList().filter { it.first == jeFile }.flatMap { it.second })
    }
    classManager.removeJar(jeFile)
}