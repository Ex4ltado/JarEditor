package me.ex4ltado.jareditor.builder

import javassist.CtMethod
import javassist.bytecode.LocalVariableAttribute

// TODO:
class CtMethodHelper(private val method: CtMethod) {

    private val localVariable: MutableMap<String, String> = mutableMapOf()

    init {
        val ca = method.methodInfo.codeAttribute
        val attribute = ca.getAttribute(LocalVariableAttribute.tag)
        if (attribute != null) {
            val lv = attribute as LocalVariableAttribute
            val len = lv.tableLength()
            for (i in 0 until len) {
                val start = lv.startPc(i)
                val end = start + lv.codeLength(i)
                val name = lv.variableName(i)
                val desc = lv.descriptor(i)
                val signature = lv.signature(i)
                val index = lv.index(i)
                //println("start: $start, end: $end, name: $name, desc: $desc, signature: $signature, index: $index")
            }
        }
    }

}