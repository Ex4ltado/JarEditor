package me.ex4ltado.jareditor

import kotlin.reflect.KClass

typealias InlineCode = CodeBuilder.() -> Unit

class Variable<T>(
    val type: Class<T>,
    val name: String,
    var value: T?,
    var expression: ArithExpr?
) {
    override fun toString(): String = name
}

class ArithExpr {

    private var expressions = ArrayDeque<String>()

    enum class Operator(val op: String, val precedence: Int) {
        ADD("+", 1),
        SUB("-", 1),
        MUL("*", 2),
        DIV("/", 2),
        MOD("%", 2)
    }

    fun group(init: ArithExpr.() -> Unit): ArithExpr {
        expressions.add("(")
        this.init()
        expressions.add(")")
        return this
    }

    fun _var(x: Variable<*>): ArithExpr {
        expressions.add(x.name)
        return this
    }

    fun _var(x: Number): ArithExpr {
        expressions.add(x.toString())
        return this
    }

    fun plus(): ArithExpr {
        expressions.add(Operator.ADD.op)
        return this
    }

    fun sub(): ArithExpr {
        expressions.add(Operator.SUB.op)
        return this
    }

    fun mul(): ArithExpr {
        expressions.add(Operator.MUL.op)
        return this
    }

    fun div(): ArithExpr {
        expressions.add(Operator.DIV.op)
        return this
    }

    fun mod(): ArithExpr {
        expressions.add(Operator.MOD.op)
        return this
    }

    operator fun ArithExpr.plus(x: ArithExpr): ArithExpr {
        adjustLastOperator(Operator.ADD)
        return this
    }

    operator fun ArithExpr.minus(x: ArithExpr): ArithExpr {
        adjustLastOperator(Operator.SUB)
        return this
    }

    operator fun ArithExpr.times(x: ArithExpr): ArithExpr {
        adjustLastOperator(Operator.MUL)
        return this
    }

    operator fun ArithExpr.div(x: ArithExpr): ArithExpr {
        adjustLastOperator(Operator.DIV)
        return this
    }

    operator fun ArithExpr.rem(x: ArithExpr): ArithExpr {
        adjustLastOperator(Operator.MOD)
        return this
    }

    private fun adjustLastOperator(op: Operator) {
        val last = expressions.removeLastOrNull() ?: return
        expressions.add(op.op)
        expressions.add(last)
    }

    fun build(): String = expressions.joinToString(" ")

}

class ArithExprBuilder {

    private val expression: String

    constructor(expression: String) {
        this.expression = expression
    }

    constructor(builder: ArithExpr.() -> Unit) {
        this.expression = ArithExpr().apply(builder).build()
    }

    fun build(): String = expression

    override fun toString(): String = build()

}

class LogicExprBuilder {
    private val expressions = mutableListOf<String>()

    fun group(init: LogicExprBuilder.() -> Unit): LogicExprBuilder {
        expressions.add("(")
        this.init()
        expressions.add(")")
        return this
    }

    fun _var(name: String, op: String, value: Int): LogicExprBuilder {
        expressions.add("$name $op $value")
        return this
    }

    fun _var(variable: Variable<*>, op: String, value: Int): LogicExprBuilder {
        expressions.add("${variable.name} $op $value")
        return this
    }

    fun _var(variable: Variable<*>, op: String, other: Variable<*>): LogicExprBuilder {
        expressions.add("${variable.name} $op ${other.name}")
        return this
    }

    fun eq(): String = "=="

    fun ne(): String = "!="

    fun gt(): String = ">"

    fun lt(): String = "<"

    fun ge(): String = ">="

    fun le(): String = "<="

    fun _and(): LogicExprBuilder {
        expressions.add("&&")
        return this
    }

    fun _or(): LogicExprBuilder {
        expressions.add("||")
        return this
    }

    fun _not(): LogicExprBuilder {
        expressions.add("!")
        return this
    }

    fun build(): String = expressions.joinToString(" ")
}

class CodeBuilder(private val instruction: StringBuilder = StringBuilder(), private var tab: Int = 0) {

    private val stack = ArrayDeque<Variable<*>>(0) // TODO
    private var insideIf = false

    private fun tabbed(function: () -> Unit) {
        tab++
        function()
        tab--
    }

    fun append(content: String): CodeBuilder {
        instruction.append(content)
        return this
    }

    fun line(content: String): CodeBuilder {
        instruction.appendLine("${"\t".repeat(tab)}$content")
        return this
    }

    fun lines(vararg content: String): CodeBuilder {
        content.forEach { line(it) }
        return this
    }

    fun jprintln(message: Any): CodeBuilder {
        call("System.out.println", message)
        return this
    }

    fun jprintln(vararg args: Any): CodeBuilder {
        line("System.out.println(${formatVararg(args, " + ")});")
        return this
    }

    fun jprintf(format: String, vararg args: Any?): CodeBuilder {
        // Javassist cant really handle varargs, so we need to use reflection
        // TODO - fix crashing the JVM
        /*lines(
            "Class.forName(\"java.io.PrintStream\")",
            ".getMethod(\"printf\", new Class[] { String.class, Object[].class });",
            ".invoke(System.out, new Object[] {\"$format\", new Object[] { ${formatVararg(args)} }});"
        )*/
        return this
    }

    fun _if(condition: String, body: InlineCode): CodeBuilder {
        insideIf = true
        line("if ($condition) {")
        tabbed {
            this.body()
        }
        line("}")
        return this
    }

    fun _else(body: InlineCode): CodeBuilder {
        if (!insideIf) throw IllegalStateException("Else without if")
        line("else {")
        tabbed {
            this.body()
        }
        line("}")
        insideIf = false
        return this
    }

    fun _elseIf(condition: String, body: InlineCode): CodeBuilder {
        line("else if($condition) {")
        tabbed {
            this.body()
        }
        line("}")
        return this
    }

    fun _try(
        exception: Class<Exception>,
        exceptionVariableName: String = "ex",
        body: InlineCode,
        catchBody: InlineCode = {},
    ): CodeBuilder {
        line("try {")
        tabbed {
            this.body()
        }
        line("} catch(${exception.name} $exceptionVariableName){")
        tabbed {
            this.catchBody()
            line("$exceptionVariableName.printStackTrace();")
        }
        line("}")
        return this
    }

    fun _while(condition: String, body: InlineCode): CodeBuilder {
        line("while ($condition) {")
        tabbed {
            this.body()
        }
        line("}")
        return this
    }

    fun _for(condition: String, body: InlineCode): CodeBuilder {
        line("for ($condition) {")
        tabbed {
            this.body()
        }
        line("}")
        return this
    }

    fun _doWhile(condition: String, body: InlineCode): CodeBuilder {
        line("do {")
        tabbed {
            this.body()
        }
        line("} while ($condition);")
        return this
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> declareOrAssignVar(
        type: Class<T>,
        name: String,
        value: T?,
        arithExpr: ArithExpr? = null
    ): Variable<T> {
        if (stack.any { it.name == name }) {
            val variable = stack.first { it.name == name && it.type == type } as Variable<T>
            variable.value = value
            variable.expression = arithExpr
            line("${variable.name} = ${arithExpr?.build() ?: value};")
            return variable
        } else {
            val element = Variable(type, name, value, arithExpr)
            stack.add(element)
            if (value != null || arithExpr != null)
                line("${type.toPrimitive()} $name = ${arithExpr?.build() ?: value};")
            else
                line("${type.toPrimitive()} $name;")
            return element
        }
    }

    inline fun <reified T> _var(name: String, value: T?): Variable<T> {
        if (T::class.isPrimitive() && value == null)
            throw IllegalArgumentException("Primitive types cannot be null")
        return declareOrAssignVar(T::class.java, name, value)
        //line("${T::class.java.toPrimitive()} $name = $value;")
        //return Variable(T::class.java, name, value, null)
    }

    inline fun <reified T> _var(name: String): Variable<T> {
        return declareOrAssignVar(T::class.java, name, null)
        //line("${T::class.java.toPrimitive()} $name;")
        //return Variable(T::class.java, name, null, null)
    }

    inline fun <reified T> _var(name: String, arithExpr: ArithExpr): Variable<T> {
        return declareOrAssignVar(T::class.java, name, null, arithExpr)
        //line("${T::class.java.toPrimitive()} $name = ${arithExpr.build()};")
        //return Variable(T::class.java, name, null, arithExpr /*TODO: evaluate on runtime*/)
    }

    fun _return(value: Any): CodeBuilder {
        line("return $value;")
        return this
    }

    fun _return(): CodeBuilder {
        line("return;")
        return this
    }

    fun _throw(exception: String): CodeBuilder {
        line("throw $exception;")
        return this
    }

    fun _throw(exception: Class<Exception>, message: String): CodeBuilder {
        line("throw new ${exception.name}(\"$message\");")
        return this
    }

    fun _throw(exception: Class<Exception>, message: Variable<String>): CodeBuilder {
        line("throw new ${exception.name}(${message.name});")
        return this
    }

    fun eval(valName: String): CodeBuilder {
        // TODO: Find the variable on the stack, evaluate it and assign the value to the variable
        return this
    }

    fun call(method: String, vararg args: Any?): CodeBuilder {
        line("$method(${formatVararg(args, ", ")});")
        return this
    }

    private fun formatVararg(args: Array<out Any?>, separator: String): String {
        val argList = args.joinToString(separator) {
            when (it) {
                is Variable<*> -> it.name
                is Array<*>, is Iterable<*> -> (it as Array<*>).joinToString(separator) { any -> any.toString() }
                else -> "\"$it\""
            }
        }
        return argList
    }

    override fun toString(): String {
        return instruction.toString()
    }

}

inline fun codeBuilder(builder: InlineCode): CodeBuilder {
    val codeBuilder = CodeBuilder()
    codeBuilder.builder() // apply the builder
    return codeBuilder
}

fun KClass<*>.isPrimitive(): Boolean {
    return when (this.java.name) {
        "java.lang.Boolean" -> true
        "java.lang.Character" -> true
        "java.lang.Byte" -> true
        "java.lang.Short" -> true
        "java.lang.Integer" -> true
        "java.lang.Float" -> true
        "java.lang.Long" -> true
        "java.lang.Double" -> true
        "java.lang.Void" -> true
        else -> false
    }
}

fun Class<*>.toPrimitive(): Class<*> {
    return when (this) {
        Integer::class.java -> Int::class.java
        java.lang.Boolean::class.java -> Boolean::class.java
        Character::class.java -> Char::class.java
        java.lang.Byte::class.java -> Byte::class.java
        java.lang.Short::class.java -> Short::class.java
        java.lang.Long::class.java -> Long::class.java
        java.lang.Float::class.java -> Float::class.java
        java.lang.Double::class.java -> Double::class.java
        else -> this
    }
}

fun logicExpr(builder: LogicExprBuilder.() -> Unit): String {
    val logicExprBuilder = LogicExprBuilder()
    logicExprBuilder.builder()
    return logicExprBuilder.build()
}

fun arithExpr(builder: ArithExpr.() -> Unit): ArithExpr {
    val arithExprBuilder = ArithExpr()
    arithExprBuilder.builder()
    return arithExprBuilder
}

