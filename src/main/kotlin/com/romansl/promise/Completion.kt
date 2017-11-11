package com.romansl.promise

import java.util.concurrent.CancellationException

class Completion<Result> {
    internal val pending = Pending<Result>()
    val promise: Promise<Result> = Promise(pending)

    fun setResult(src: Result) {
        complete(promise, pending, Succeeded(src))
    }

    fun setError(src: Exception) {
        complete(promise, pending, Failed(src))
    }

    fun setCancelled() {
        complete(promise, pending, Failed(CancellationException()))
    }
}
