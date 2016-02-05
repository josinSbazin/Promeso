package com.romansl.promise

class Completion<Result> internal constructor(val promise: Promise<Result>) {
    fun setResult(src: Result) {
        val state = Succeeded(src)
        promise.state.getAndSet(state).complete(state)
    }

    fun setError(src: Exception) {
        val state = Failed<Result>(src)
        promise.state.getAndSet(state).complete(state)
    }
}
