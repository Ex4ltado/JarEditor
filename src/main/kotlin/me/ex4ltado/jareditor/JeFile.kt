package me.ex4ltado.jareditor

import me.ex4ltado.jareditor.util.LoggerFactory
import me.ex4ltado.jareditor.util.toJavaName
import me.ex4ltado.jareditor.util.withSuffix
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.util.jar.JarFile
import java.util.jar.Manifest

private const val DEFAULT_BUFFER_SIZE = 8 * 1024
private const val MANIFEST_MAIN_CLASS = "Main-Class"

class JeFile(private val jar: JarFile) {

    enum class DataReadType {
        /**
         * Still reading the data
         */
        READING,

        /**
         * Caught an exception while reading the data
         */
        EXCEPTION,

        /**
         * Done reading the data
         */
        DONE
    }

    private val logger = LoggerFactory.getLogger(this::class.java)

    val jarName = jar.name.substring(jar.name.lastIndexOf(File.separatorChar) + 1).replace(".jar", "")

    var read = false
        private set

    private val dataEntries: MutableList<DataEntry> = mutableListOf()

    private val manifest: Manifest = jar.manifest

    fun read(onDataReadCallback: ((String, DataReadType) -> Unit)? = null) {
        synchronized(this) {
            if (read) throw IllegalStateException("Jar file already read")
            jar.entries().asSequence().forEach {
                jar.getInputStream(it).use { inputStream ->
                    val entry = DataEntry(it.name)
                    entry.read(inputStream, onDataReadCallback)
                    dataEntries.add(entry)
                }
            }
            jar.close()
            read = true
        }
    }

    internal fun getEntry(name: String): DataEntry? = dataEntries.find { it.name == name }

    internal fun getEntries(): List<DataEntry> = dataEntries.toList()

    fun getClassList(): List<JeClass> = dataEntries.filter { it.isClass() }.map { it.toPair() }

    fun modifyOrPutClass(className: String, bytecode: ByteArray) {
        val cafebabe = byteArrayOf(0xCA.toByte(), 0xFE.toByte(), 0xBA.toByte(), 0xBE.toByte())
        if (bytecode.size < 4 || !bytecode.sliceArray(0..3).contentEquals(cafebabe)) {
            throw IllegalArgumentException("Invalid class file")
        }
        //dataEntries.addOrUpdate(DataEntry(className.normalizeToClassEntry(), bytecode))
    }

    fun deleteEntry(name: String) {
        dataEntries.removeIf { it.name == name.toJavaName() }
    }

    fun deleteClass(className: String) {
        dataEntries.removeIf { it.name == className.toJavaName() }
    }

    fun getManifestAttribute(key: String): String? = manifest.mainAttributes.getValue(key)

    fun remapClassPackage(path: String) {
        TODO()
    }

    fun getRandomPackage(): String {
        var path = dataEntries.filter { it.isClass() }.random().name
        if (path.count { it == '/' } > 0)
            path = path.substring(0, path.lastIndexOf("/"))
        else
            return ""
        return "$path/"
    }

    fun findClass(classPath: String): JeClass? {
        val entry =
            dataEntries.filter { it.isClass() }.firstOrNull { it.name == classPath.withSuffix(".class") }
        if (entry == null) {
            logger.info("Class $classPath not found in jar $jarName")
            return null
        }
        return entry.toPair()
    }

    /**
     * Find the main class according to [manifest] attribute
     *
     * @return Pair with the class name and the bytecode
     */
    fun mainClass(): JeClass? {
        val mainClassPath = getManifestAttribute(MANIFEST_MAIN_CLASS)!!.toJavaName()
        return findClass(mainClassPath)
    }

    internal class DataEntry(val name: String) {

        private val logger = LoggerFactory.getLogger(this::class.java)

        lateinit var data: ByteArray

        constructor(name: String, byteArray: ByteArray) : this(name) {
            this.data = byteArray
        }

        fun read(inputStream: InputStream, onDataReadCallback: ((String, DataReadType) -> Unit)? = null) {
            if (this::data.isInitialized && data.isNotEmpty()) {
                logger.info("Data already initialized for entry $name")
                return
            }
            runCatching {
                inputStream.use {
                    val bos = ByteArrayOutputStream()
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var len = it.read(buffer)
                    while (len > 0) {
                        bos.write(buffer, 0, len)
                        len = it.read(buffer)
                        onDataReadCallback?.invoke(name, DataReadType.READING)
                    }
                    data = bos.toByteArray()
                    onDataReadCallback?.invoke(name, DataReadType.DONE)
                }
            }.onFailure { e ->
                onDataReadCallback?.invoke(name, DataReadType.EXCEPTION)
                throw e
            }
        }

        fun isClass(): Boolean = name.endsWith(".class")

        fun toPair(): JeClass = name to data

    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is JeFile) return false

        if (jar != other.jar) return false
        if (jarName != other.jarName) return false
        if (manifest != other.manifest) return false

        return true
    }

    override fun hashCode(): Int {
        var result = jar.hashCode()
        result = 31 * result + jarName.hashCode()
        result = 31 * result + manifest.hashCode()
        return result
    }

}

private fun MutableList<JeFile.DataEntry>.addOrUpdate(dataEntry: JeFile.DataEntry) {
    val index = this.indexOfFirst { it.name == dataEntry.name }
    if (index != -1) {
        this[index] = dataEntry
    } else {
        this.add(dataEntry)
    }
}
