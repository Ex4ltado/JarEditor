package me.ex4ltado.jareditor.transform

abstract class Transform<A, B> {

    abstract fun transform(obj: A): B

}