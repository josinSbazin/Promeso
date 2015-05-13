package com.romansl.promise

import java.util.concurrent.Executors

fun main(args: Array<String>) {
    println("Start")

    val tcs = Promise.create<Int>()

    tcs.task.then {
        println("Then")
        try {
            println("result: $result")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    //tcs.result = 10
    tcs.error = RuntimeException("UJOSSS")

    val executor = Executors.newCachedThreadPool()
    Promise.call(executor) {
        println("1")
        Thread.sleep(1000)
        println("2")
    }.then {
        println("Then2")
        throw RuntimeException("UJJJJJOOOOSSS")
    }.then {
        println("Then3")
        try {
            println("result: $result")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    println("End")
}

