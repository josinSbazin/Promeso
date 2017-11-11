package com.romansl.promise

import java.util.concurrent.CountDownLatch

class CompletedLazy<Result> : Completed<Result>() {
    private val latch = CountDownLatch(1)
    private lateinit var finiteState: Completed<Result>

    override val result: Result
        get() {
            latch.await()
            return finiteState.result
        }

    internal fun resolveState(completed: Completed<Result>) {
        finiteState = completed
        latch.countDown()
    }
}
