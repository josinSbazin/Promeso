package com.romansl.promise

internal class ThenFlattenListener<in T>(
        private val promise: Promise<T>,
        private val pending: Pending<T>) : (Completed<T>) -> Unit {
    override fun invoke(completed: Completed<T>) {
        complete(promise, pending, completed)
    }
}
