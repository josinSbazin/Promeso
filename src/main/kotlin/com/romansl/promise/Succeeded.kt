package com.romansl.promise

public class Succeeded<out T>(override val result: T) : Completed<T>()
