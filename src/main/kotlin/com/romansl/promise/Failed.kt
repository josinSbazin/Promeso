package com.romansl.promise

public class Failed<T>(private val exception: Exception) : Completed<T>() {
    override val result: T
        get() = throw exception
}
