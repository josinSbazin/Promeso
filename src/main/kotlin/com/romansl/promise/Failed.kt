package com.romansl.promise

class Failed<out T>(private val exception: Exception) : Completed<T>() {
    override val result: T
        get() = throw exception
}
