package me.ex4ltado.jareditor.util

class MaxSizeArrayList<E>(private val maxSize: Int, initialCapacity: Int = 0) : ArrayList<E>(initialCapacity) {

    init {
        require(maxSize > 0) { "maxSize must be greater than 0" }
    }

    override fun add(element: E): Boolean {
        if (size >= maxSize) removeAt(0)
        return super.add(element)
    }

    override fun add(index: Int, element: E) {
        if (size >= maxSize) removeAt(0)
        super.add(index, element)
    }

    override fun addAll(elements: Collection<E>): Boolean {
        if (size + elements.size > maxSize) {
            val toRemove = size + elements.size - maxSize
            removeRange(0, toRemove)
        }
        return super.addAll(elements)
    }

}