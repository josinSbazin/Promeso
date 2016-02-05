package com.romansl.promise

class Succeeded<out T>(override val result: T) : Completed<T>()
