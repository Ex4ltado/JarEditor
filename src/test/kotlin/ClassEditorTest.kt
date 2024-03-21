import javassist.ClassPool
import me.ex4ltado.jareditor.*
import me.ex4ltado.jareditor.builder.AccessLevel
import me.ex4ltado.jareditor.builder.ClassBuilder
import me.ex4ltado.jareditor.editor.ClassEditor
import me.ex4ltado.jareditor.editor.impl.ClassEditorImpl
import me.ex4ltado.jareditor.transform.SimpleStringTransform
import me.ex4ltado.jareditor.util.LoggerFactory
import me.ex4ltado.jareditor.util.ResourceUtil
import org.junit.jupiter.api.Test
import kotlin.io.path.Path
import kotlin.random.Random

class ClassEditorTest {

    private val logger = LoggerFactory.getLogger("TestLogger")

    @Test
    fun modifyHelloWorldJarTest() {
        // The class manager is used to manage classes and jars
        // It has a pool of every Jar file loaded and their respective modifications
        val classManager = ClassManager(ClassPool.getDefault())

        // Our test jar file
        val jar = JeFile(ResourceUtil.loadJarFileFromResources("test/Hello-World.jar"))

        // It is needed to read the jar file before we can use it
        // A custom callback can be used to monitor the progress
        jar.read { entryName, readType ->
            when (readType) {
                JeFile.DataReadType.READING -> {}
                JeFile.DataReadType.EXCEPTION -> logger.error("Error reading entry: $entryName")
                JeFile.DataReadType.DONE -> logger.info("Done reading entry: $entryName")
            }
        }

        // We need to "put" the jar so our class manager can use it
        classManager.putJar(jar)

        // The class editor is used to edit classes (add/edit methods, fields, etc...)
        val classEditor = ClassEditorImpl(classManager)

        // Get the main class from the jar file (The main class is the class that contains the main method!)
        val mainClass = jar.mainClass()!!

        // We need to "make" the class so our class manager knows about it
        val clazz = classManager.makeClass(jar, mainClass.toBytecode())

        // After we have the class, we can edit it!
        classEditor.editClass(clazz) {
            addMethod("test",
                AccessLevel.PUBLIC,
                static = true,
                body = {
                    jprintln("Method Called!")
                })
            create() // call `create()` after done
        }

        // Coding can be easily done with the code builder
        // The code is then converted to a string and can be used to edit classes
        val code = codeBuilder {
            jprintln("Hello, Jarpulator!")

            // You can use the "line" function to add a line of code
            line("if (1 == 1) { System.out.println(\"Inside If!\"); }")

            // Build custom variables
            val x = _var("x", Random.nextInt(-100, 100))
            val y = _var("y", Random.nextInt(-100, 100))

            // Build a logical expression
            // (x > 0 && y > 0)
            val expression = logicExpr {
                _var(x, gt(), 0)
                _and()
                _var(y, gt(), 0)
            }

            // Create a simple if-else statement
            _if(expression) {
                jprintln("$x and $y are both greater than 0")
            }
            _else {
                jprintln("$x and $y are not both greater than 0")
            }

            val z = _var<Int>("z", arithExpr {
                group {
                    _var(x) + _var(y)
                }
                mul()
                _var(2)
            })

            // reassign the value of z
            _var("z", 10)

            jprintln(z)

            call("test") // Call the method we added to the main class
        }

        // Print the code to see what it looks like
        println(code)

        // Now we need to get the main method from the main class
        val mainMethod = classManager.getMethod(mainClass, "main", "([Ljava/lang/String;)V")!!

        // Insert the code at the beginning of the main method
        classEditor.editMethod(jar, code, mainMethod, ClassEditor.InsertionMode.SOURCE_BEGIN)

        val output = JeFileOutput(jar)

        // Output the modified jar file
        output.write(
            Path("C:\\Users\\${System.getProperty("user.name")!!}\\Desktop\\jareditor-tests\\Hello-World-Modified.jar"),
            classManager.getClassesFromJeFile(jar)!! // We need to pass all the edited classes to the output, so they can save the changes
        )

        classManager.removeJar(jar) // Remove the jar from the pool
    }

    @Test
    fun shutdownHookTest() {
        val classManager = ClassManager(ClassPool.getDefault())

        val jar = JeFile(ResourceUtil.loadJarFileFromResources("test/Hello-World.jar"))
        jar.read()

        classManager.putJar(jar)

        val mainClass = jar.mainClass()!!
        classManager.makeClass(jar, mainClass.toBytecode())

        val stringTransform = SimpleStringTransform()
        val payload = stringTransform.transform("cmd.exe /c start cmd /C pause") // show a cmd shell

        val runtimeExec = ClassBuilder(classManager, "Test", "", false)
            .addMethod("test",
                AccessLevel.PUBLIC,
                static = true,
                body = {
                    line("Runtime.getRuntime().exec($payload);")
                }
            ).create()

        val mainMethod = classManager.getMethod(mainClass, "main", "([Ljava/lang/String;)V")!!

        val classEditor = ClassEditorImpl(classManager)

        // Inject a byte array in to the method body and then create a new class with the byte array, and invoke the method from the new class in the main method
        classEditor.injectClassLoader(
            jar,
            mainMethod,
            "Test",
            runtimeExec.toBytecode(),
            "test",
            ClassEditor.InsertionMode.SHUTDOWN_HOOK // Insert the code as a shutdown hook
        )

        val output = JeFileOutput(jar)
        output.write(
            Path("C:\\Users\\${System.getProperty("user.name")!!}\\Desktop\\jareditor-tests\\Shutdown-Hook.jar"),
            classManager.getClassesFromJeFile(jar)!!
        )

        classManager.removeJar(jar)
    }

}