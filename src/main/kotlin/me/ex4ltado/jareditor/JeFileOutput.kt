package me.ex4ltado.jareditor

import javassist.CtClass
import me.ex4ltado.jareditor.util.toJavaName
import java.io.FileOutputStream
import java.nio.file.Path
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import kotlin.io.path.createDirectories
import kotlin.io.path.notExists

class JeFileOutput(private val jeFile: JeFile) {

    // A map to hold the entries of the Jar file. The key is the entry name and the value is the file data.
    private val outputMap = HashMap<String, ByteArray>()

    fun write(path: Path, editedList: List<CtClass>? = null) {

        val iterator = jeFile.getEntries().iterator()

        while (iterator.hasNext()) {
            val entry = iterator.next()
            outputMap[entry.name] = entry.data
        }

        editedList?.forEach {
            outputMap[it.name.toJavaName()] = it.toBytecode()
        }

        if (path.notExists())
            path.parent.createDirectories()

        FileOutputStream(path.toFile()).use { fileOutputStream ->
            JarOutputStream(fileOutputStream).use { outputStream ->
                outputMap.forEach {
                    val entry = JarEntry(it.key)
                    outputStream.putNextEntry(entry)
                    outputStream.write(it.value)
                    outputStream.closeEntry()
                }
            }
        }
    }

    private fun sanitizePath(path: Path): Path {
        var newPath = path
        if (!newPath.isAbsolute) {
            newPath = newPath.toAbsolutePath()
        }
        newPath = newPath.normalize()
        return newPath
    }

}