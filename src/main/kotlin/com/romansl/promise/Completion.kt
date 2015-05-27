package com.romansl.promise

public class Completion<Result> internal(public val promise: Promise<Result>) {
    public fun setResult(src: Result) {
        val state = Succeeded(src)
        promise.state.getAndSet(state).complete(state)
    }

    public fun setError(src: Exception) {
        val state = Failed<Result>(src)
        promise.state.getAndSet(state).complete(state)
    }
}
