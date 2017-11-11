package com.romansl.promise

class Failed(val exception: Exception) : Completed<Any>() {
    override val result: Nothing
        get() = throw exception
}
