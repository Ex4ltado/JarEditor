package me.ex4ltado.jareditor.util

import java.util.*

enum class NameType {
    LOWERCASE,
    UPPERCASE,
    NUMBERS;

    infix fun or(other: NameType): EnumSet<NameType> = NameTypes.of(this, other)
}

typealias NameTypes = EnumSet<NameType>

infix fun NameTypes.allOf(other: NameTypes) = this.containsAll(other)
infix fun NameTypes.or(other: NameType): EnumSet<NameType> = NameTypes.of(other, *this.toTypedArray())
infix fun NameTypes.and(other: NameType) = this.contains(other)

object ClassNameUtil {

    private val numbers = ('0'..'9').toText()
    private val lowercaseAlphabet = ('a'..'z').toText()
    private val uppercaseAlphabet = ('A'..'Z').toText()

    private fun getRandomName(length: Int, mode: NameTypes): String {
        var allowedCharacters = ""

        if (mode and NameType.LOWERCASE)
            allowedCharacters += lowercaseAlphabet
        if (mode and NameType.UPPERCASE)
            allowedCharacters += uppercaseAlphabet
        if (mode and NameType.NUMBERS)
            allowedCharacters += numbers

        if (allowedCharacters.isEmpty())
            throw IllegalArgumentException("mode must be at least one of the following: LOWERCASE, UPPERCASE, NUMBERS")

        val randomString = StringBuilder()
        for (i in 0 until length) {
            randomString.append(allowedCharacters.random())
        }

        return randomString.toString()
    }

    private fun getRandomName(length: Int, mode: NameType): String {
        return getRandomName(length, NameTypes.of(mode))
    }

    private fun randomPackageName(length: Int): String {
        return getRandomName(length, NameType.LOWERCASE)
    }

    fun randomClassName(length: Int, numberOfPackages: Int): String {
        val className = StringBuilder()
        for (i in 0 until numberOfPackages) {
            className.append(randomPackageName((Math.random() * length + 1).toInt()))
            className.append(".")
        }
        className.append(getRandomName(1, NameType.UPPERCASE))
        className.append(getRandomName(length - 1, NameType.LOWERCASE or NameType.UPPERCASE or NameType.NUMBERS))
        return className.toString()
    }

    fun randomMethodName(length: Int): String {
        if (length < 2) throw IllegalArgumentException("length must be greater than 1")
        return getRandomName(1, NameType.LOWERCASE) + getRandomName(length - 1, NameType.LOWERCASE or NameType.NUMBERS)
    }

}

private fun CharRange.toText(): String {
    return this.joinToString("")
}
