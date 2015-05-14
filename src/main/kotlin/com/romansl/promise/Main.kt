package com.romansl.promise

import java.util.concurrent.Executors

fun main(args: Array<String>) {
    val executor = Executors.newSingleThreadExecutor()
    val promise = Promise.succeeded(10)
    promise.after(executor) {
        val r = result
        println(r)
        Promise.succeeded(r + 11)
    }.after<Int>(executor) {
        println(result)
        throw RuntimeException("eee")
    }.then(executor) {
        println("pre-fin")
        result
    }.then {
        println("fin")
    }

    val completion = Promise.create<Int>()
    completion.promise.after(executor) {
        val r = result
        println(r)
        Promise.succeeded(r + 11)
    }.after<Int>(executor) {
        println(result)
        throw RuntimeException("uuu")
    }.then(executor) {
        println("pre-fin2")
        result
    }.then {
        println("fin2")
    }
    completion.result = 100
}

