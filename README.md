## Jarpulator ⚡️

Jarpulator is a Java/Kotlin lib that uses Javassist to decompile, modify and recompile .jar files.

allows you to decompile, modify and recompile .jar files.

<!-- TOC -->

* [Jarpulator ⚡️](#jarpulator-)
    * [Use Example](#use-example)
    * [📝 Table of Contents](#-table-of-contents)
    * [Images](#images)

<!-- TOC -->

---

### Use Example

Given the following Java code in a file called `Hello-World.jar`:

```java
public class Main {
    public static void main(String[] args) {
        System.out.println("Hello, World!");
    }
}
```

And then with the following Kotlin code you can load the jar `Hello-World.jar`:

```kotlin
// The class manager is used to manage classes and jars
// It has a pool of every Jar file loaded and their respective modifications
val classManager = ClassManager(ClassPool.getDefault())

// In this example, we are loading the jar file from the resources, but it can be loaded from anywhere
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
```

You first need to find the class that you will modify from the jar file and then "make" the class so the class manager
knows about it.

```kotlin
// Get the main class from the jar file (The main class is the class that contains the main method!)
val mainClass = jar.mainClass()!!

// We need to "make" the class so our class manager knows about it
val clazz = classManager.makeClass(jar, mainClass.toBytecode())
```

After that you can modify the bytecode of the `main` method to print `Hello, Jarpulator!` before the first line of the
method:

```kotlin
// First, get the method that we want to modify (In this case, the main method)
val mainMethod = classManager.getMethod(mainClass, "main", "([Ljava/lang/String;)V")!!

// The class editor is used to edit classes (add/edit methods, fields, etc...)
val classEditor = ClassEditorImpl(classManager)

// Coding can be easily done with the code builder
// The code is then converted to a string and can be used to edit classes
val code = codeBuilder {
    jprintln("Hello, Jarpulator!")
}

// Insert the code at the beginning of the main method
classEditor.editMethod(jar, code, mainMethod, ClassEditor.InsertionMode.SOURCE_BEGIN)
```

After that, you can recompile the modified jar file:

```kotlin
val output = JeFileOutput(jar)

// Output the modified jar file
output.write(
    Path("C:\\Users\\${System.getProperty("user.name")!!}\\Desktop\\java-hello-world\\Hello-World-Modified.jar"),
    classManager.getClassesFromJeFile(jar)!! // We need to pass all the edited classes to the output, so they can save the changes
)
```

The output of the modified runnable jar file will be:

```java
public class Main {
    public static void main(String[] args) {
        System.out.println("Hello, Jarpulator!");
        System.out.println("Hello, World!");
    }
}
```

When you are done, you can remove it from the class manager, so it can be garbage collected:

```kotlin
classManager.removeJar(jar)
```

---

### More Examples

Modifications can also be done, like adding new methods, fields, implementing interfaces, inherit other classes, etc...

```kotlin
val clazz = classManager.makeClass(jar, mainClass.toBytecode())

/**
 * add the following method to the class:
 *
 * public static void test() {
 *     System.out.println("Method Called!");
 * }
 */
classEditor.editClass(clazz) {
    addMethod("test",
        AccessLevel.PUBLIC,
        static = true,
        body = {
            jprintln("Method Called!")
        })
    create() // call `create()` after done
}
```

More "complex" code also can be added:

```kotlin
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
    }.build()

    // Create a simple if-else statement
    _if(expression) {
        jprintln("$x and $y are both greater than 0")
    }
    _else {
        jprintln("$x and $y are not both greater than 0")
    }

    // declare a variable with an arithmetic expression
    // int z = (x + y) * 2
    val z = _var<Int>("z", arithExpr {
        group {
            _var(x) + _var(y)
        }
        mul()
        _var(2)
    })

    jprintln(z)

    // reassign the value of z
    _var("z", 10)

    jprintln(z)

    call("test") // Call the method we added above
}
```

After that, you can recompile the modified jar and the output will be something like this:

```java
public class Main {
    public static void main(String[] args) {
        System.out.println("Hello, Jarpulator!");
        if (true)
            System.out.println("Inside If!");
        int x = -93;
        int y = 65;
        if (x <= 0 || y <= 0) {
            System.out.println("x and y are not both greater than 0");
        } else {
            System.out.println("x and y are both greater than 0");
        }
        int z = (x + y) * 2;
        System.out.println(z);
        z = 10;
        System.out.println(z);
        test();
        System.out.println("Hello world!");
    }

    public static void test() {
        System.out.println("Method Called!");
    }
}
```

More examples can be found [here](src/test/kotlin/ClassEditorTest.kt).

---

### 📝 Table of Contents

- [x] Decompile .jar files
- [x] Classes viewer in the App
- [x] .jar file modification
- [x] Output modified .jar file
- [ ] Console version
- [ ] Rich Text Editor
- [ ] Class Editor in the App
- [ ] Settings menu (change decompiler, switch themes, etc.)
- [ ] Keyboard shortcuts
- [ ] Show the decompiled bytecode
- [ ] Merge 2 or more .jar files
- [ ] Configuration file
- [ ] .war support
- [ ] more decompilers

---

### App

The app is still in development, but it is already possible to decompile .jar files and view the classes.

App source code can be found [here](src/main/java/me/ex4ltado/jareditor/app).

![](/images/EmptyGUI.png)

![](/images/DecompiledJar.png)

![](/images/ModifiedJar.png)