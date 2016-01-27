package com.romansl.promise

internal class ThenCompleteListener<T>(private val completion: Completion<T>) : (Completed<T>) -> Unit {
    override fun invoke(completed: Completed<T>) {
        completion.promise.state.getAndSet(completed).complete(completed)
    }
}
