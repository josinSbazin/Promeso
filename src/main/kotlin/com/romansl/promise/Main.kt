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

    promise.then(executor) {
        val r = result
        //println(r)
        r + 11
        Thread.yield()
    }.then<Int>(executor) {
        Thread.yield()
        latch.countDown()
        10
        //println(result)
        //throw RuntimeException("eee")
    }/*.then(executor) {
        //println("pre-fin")
        result
    }.then(executor) {
        //println("fin")
        latch.countDown()
    }*/

    //    val completion = Promise.create<Int>()
    //    completion.promise.after(executor) {
    //        val r = result
    //        println(r)
    //        Promise.succeeded(r + 11)
    //    }.after<Int>(executor) {
    //        println(result)
    //        throw RuntimeException("uuu")
    //    }.then(executor) {
    //        println("pre-fin2")
    //        result
    //    }.then {
    //        println("fin2")
    //        latch.countDown()
    //    }

    //    completion.result = 100

    latch.await()
    //executor.shutdown()
}

