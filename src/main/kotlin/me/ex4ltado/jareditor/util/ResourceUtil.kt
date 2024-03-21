package me.ex4ltado.jareditor.util

import java.io.File
import java.io.FileOutputStream
import java.util.jar.JarFile

object ResourceUtil {

    fun loadFileFromResourcesAsStream(
        name: String,
        suffix: String = ".tmp",
        deleteOnExit: Boolean = true
    ): File {
        val inputStream = ResourceUtil::class.java.classLoader.getResourceAsStream(name)!!

        val tempFile = File.createTempFile(inputStream.hashCode().toString(), suffix)

        if (deleteOnExit) tempFile.deleteOnExit()

        FileOutputStream(tempFile).use { out ->
            val buffer = ByteArray(1024)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                out.write(buffer, 0, bytesRead)
            }
        }
        return tempFile
    }

    fun loadFromResources(name: String): ByteArray =
        ResourceUtil::class.java.classLoader.getResourceAsStream(name)!!.readBytes()

    fun loadFileFromResources(name: String): File =
        File(ResourceUtil::class.java.classLoader.getResource(name)!!.path)

    fun loadJarFileFromResources(name: String): JarFile =
        JarFile(ResourceUtil::class.java.classLoader.getResource(name)!!.toURI().path)

}