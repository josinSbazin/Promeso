package com.romansl.promise

public class Succeeded<T>(override val result: T) : Completed<T>()
