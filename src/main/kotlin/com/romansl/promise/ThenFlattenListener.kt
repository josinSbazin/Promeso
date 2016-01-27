package com.romansl.promise

internal class ThenFlattenListener<T>(private val promise: Promise<T>) : (Completed<T>) -> Unit {
    override fun invoke(completed: Completed<T>) {
        promise.state.getAndSet(completed).complete(completed)
    }
}
