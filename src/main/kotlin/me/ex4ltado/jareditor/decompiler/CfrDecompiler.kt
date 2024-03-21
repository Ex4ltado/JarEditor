package me.ex4ltado.jareditor.decompiler

import me.ex4ltado.jareditor.*
import me.ex4ltado.jareditor.decompiler.Decompiler.DecompilationResult
import me.ex4ltado.jareditor.util.toJvmName
import org.benf.cfr.reader.api.CfrDriver
import org.benf.cfr.reader.api.ClassFileSource
import org.benf.cfr.reader.api.OutputSinkFactory
import org.benf.cfr.reader.api.OutputSinkFactory.*
import org.benf.cfr.reader.api.SinkReturns.DecompiledMultiVer
import org.benf.cfr.reader.api.SinkReturns.ExceptionMessage
import java.io.IOException
import java.io.StringWriter
import java.util.*
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair as CfrPair

class CfrDecompiler(private val classManager: ClassManager) : Decompiler {

    private val classFileSource: ClassFileSource = JarEditorClassFileSourceImpl { name ->
        classManager.classPool[name]?.toBytecode()?.let { bytecode -> JeClass(name, bytecode) }
    }

    private val options: HashMap<String, String> = HashMap()

    private var stringBuilders = false
    private var stringSwitches = false
    private var tryWith = false
    private var lambdas = false
    private var finallies = false
    private var hideLongStrings = false
    private var hideUTF8 = false
    private var removeSynthetic = false
    private var commentMonitors = false
    private var topsort = false
    private var ignoreExcpetions = false

    init {
        options["aexagg"] = "false"
        options["allowcorrecting"] = "true"
        options["arrayiter"] = "true"
        options["caseinsensitivefs"] = "false"
        options["clobber"] = "false"
        options["collectioniter"] = "true"
        options["commentmonitors"] = "false"
        options["decodeenumswitch"] = "true"
        options["decodefinally"] = finallies.toString()
        options["decodelambdas"] = lambdas.toString()
        options["decodestringswitch"] = stringSwitches.toString()
        options["dumpclasspath"] = "false"
        options["eclipse"] = "true"
        options["elidescala"] = "false"
        options["forcecondpropagate"] = "false"
        options["forceexceptionprune"] = "false"
        options["forcereturningifs"] = "false"
        options["forcetopsort"] = topsort.toString()
        options["forcetopsortaggress"] = topsort.toString()
        options["forloopaggcapture"] = "false"
        options["hidebridgemethods"] = "true"
        options["hidelangimports"] = "true"
        options["hidelongstrings"] = hideLongStrings.toString()
        options["hideutf"] = hideUTF8.toString()
        options["ignoreexceptionsalways"] = ignoreExcpetions.toString()
        options["innerclasses"] = "true"
        options["j14classobj"] = "false"
        options["labelledblocks"] = "true"
        options["lenient"] = "false"
        options["liftconstructorinit"] = "true"
        options["override"] = "true"
        options["pullcodecase"] = "false"
        options["recover"] = "true"
        options["recovertypeclash"] = "false"
        options["recovertypehints"] = "false"
        options["relinkconststring"] = "true"
        options["removebadgenerics"] = "true"
        options["removeboilerplate"] = "true"
        options["removedeadmethods"] = "true"
        options["removeinnerclasssynthetics"] = removeSynthetic.toString()
        options["rename"] = "false"
        options["renamedupmembers"] = "false"
        options["renameenumidents"] = "false"
        options["renameillegalidents"] = "false"
        options["showinferrable"] = "false"
        options["showversion"] = "false"
        options["silent"] = "false"
        options["stringbuffer"] = stringBuilders.toString()
        options["stringbuilder"] = stringBuilders.toString()
        options["sugarasserts"] = "true"
        options["sugarboxing"] = "true"
        options["sugarenums"] = "true"
        options["tidymonitors"] = "true"
        options["commentmonitors"] = commentMonitors.toString()
        options["tryresources"] = tryWith.toString()
        options["usenametable"] = "true"
    }

    override fun decompile(jeFile: JeFile, classPath: String): DecompilationResult? {

        val summaryOutput = StringWriter()
        val summarySink =
            Sink<String?>(summaryOutput::append) // Messages include line terminator, therefore, only print

        val exceptionsOutput = StringWriter()
        val exceptionSink = Sink { exceptionMessage: ExceptionMessage ->
            exceptionsOutput.append(exceptionMessage.path).append('\n')
            exceptionsOutput.append(exceptionMessage.message).append('\n')

            val exception = exceptionMessage.thrownException
            exceptionsOutput
                .append(exception.javaClass.name)
                .append(": ")
                .append(exception.message)
                .append("\n\n")
        }

        val decompiledList: MutableList<DecompiledMultiVer> = ArrayList()
        val decompiledSourceSink = Sink(decompiledList::add)

        val sinkFactory = object : OutputSinkFactory {
            override fun getSupportedSinks(sinkType: SinkType, collection: Collection<SinkClass?>): List<SinkClass?>? {
                return when (sinkType) {
                    SinkType.JAVA -> Collections.singletonList(SinkClass.DECOMPILED_MULTIVER)
                    SinkType.EXCEPTION -> Collections.singletonList(SinkClass.EXCEPTION_MESSAGE)
                    SinkType.SUMMARY -> Collections.singletonList(SinkClass.STRING)
                    else ->
                        Collections.singletonList(SinkClass.STRING)
                }
            }

            @Suppress("UNCHECKED_CAST")
            private fun <T> castSink(sink: Sink<*>): Sink<T> {
                return sink as Sink<T>
            }

            override fun <T> getSink(sinkType: SinkType?, sinkClass: SinkClass): Sink<T> {
                when (sinkType) {
                    SinkType.JAVA -> {
                        require(sinkClass == SinkClass.DECOMPILED_MULTIVER) { "Sink class $sinkClass is not supported for decompiled output" }
                        return castSink(decompiledSourceSink)
                    }

                    SinkType.EXCEPTION -> return when (sinkClass) {
                        SinkClass.EXCEPTION_MESSAGE -> castSink(exceptionSink)
                        SinkClass.STRING -> castSink(summarySink)
                        else -> throw IllegalArgumentException("Sink factory does not support $sinkClass")
                    }

                    SinkType.SUMMARY -> return castSink(summarySink)
                    else -> return Sink { _: T -> }
                }
            }
        }

        val driver = CfrDriver.Builder()
            .withOptions(options)
            .withOutputSink(sinkFactory)
            .withClassFileSource(classFileSource)
            .build()

        driver.analyse(listOf(classPath))

        // Replace version information and file path to prevent changes in the output
        //val summary: String = summaryOutput.toString().replace(CfrVersionInfo.VERSION_INFO, "<version>")
        //    .replace(pathString, "<path>/" + path.getFileName().toString())

        val summary: String = summaryOutput.toString()

        if (decompiledList.isEmpty()) return null

        return DecompilationResult(summary, exceptionsOutput.toString(), decompiledList.first())
    }

}

class JarEditorClassFileSourceImpl(val classFileContentRetriever: (String) -> JeClass?) : ClassFileSource {

    override fun informAnalysisRelativePathDetail(usePath: String?, classFilePath: String?) {
    }

    override fun getPossiblyRenamedPath(path: String): String {
        return path
    }

    @Throws(IOException::class)
    override fun getClassFileContent(path: String): CfrPair<ByteArray, String>? {
        return classFileContentRetriever(path.toJvmName())?.let {
            CfrPair.make(it.toBytecode(), it.name())
        }
    }

    override fun addJar(jarPath: String?): Collection<String?>? {
        throw UnsupportedOperationException("Never should be called")
    }

}
