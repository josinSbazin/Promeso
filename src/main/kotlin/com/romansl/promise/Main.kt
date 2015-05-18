package com.romansl.promise

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

val executor = Executors.newSingleThreadExecutor()

fun main(args: Array<String>) {
    for(i in 1..100){
        ggg()
    }
    executor.shutdown()
}

private fun ggg() {
    val promise = Promise.succeeded(10)
    val latch = CountDownLatch(1)

    promise.isPending {
        println("pending")
    }.then(executor) {
        val r = result
        println(r)
        r + 11
    }.then<Int>(executor) {
        println(result)
        throw RuntimeException("eee")
    }.then(executor) {
        println("pre-fin")
        result
    }.then(executor) {
        println("fin")
        latch.countDown()
    }

    latch.await()
}

