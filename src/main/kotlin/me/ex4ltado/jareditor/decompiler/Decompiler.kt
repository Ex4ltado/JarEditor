package me.ex4ltado.jareditor.decompiler

import me.ex4ltado.jareditor.JeFile
import org.benf.cfr.reader.api.SinkReturns.DecompiledMultiVer

interface Decompiler {

    data class DecompilationResult(
        val summary: String,
        val exceptions: String,
        val decompiled: DecompiledMultiVer
    )

    fun decompile(
        jeFile: JeFile,
        classPath: String,
        //classData: ByteArray
    ): DecompilationResult? // TODO: Change to jarPath: String, classPath: String

}