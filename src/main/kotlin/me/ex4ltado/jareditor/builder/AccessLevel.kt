package me.ex4ltado.jareditor.builder

enum class AccessLevel(val modifier: String) {
    PUBLIC("public"), PRIVATE("private"), PROTECTED("protected"), PACKAGE("")
}