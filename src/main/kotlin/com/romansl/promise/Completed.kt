package com.romansl.promise

import java.util.concurrent.Executor
import kotlin.coroutines.experimental.Continuation

abstract class Completed<out T> : State<T>() {
    abstract val result: T

    override fun complete(newState: Completed<*>) {
        // no op
    }

    override fun thenContinuation(continuation: Continuation<T>) {
        try {
            continuation.resume(result)
        } catch (e: OutOfMemoryError) {
            continuation.resumeWithException(InterceptedOOMException(e))
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }

    override fun thenContinuation(continuation: Continuation<T>, executor: Executor) {
        executor.execute {
            try {
                continuation.resume(result)
            } catch (e: OutOfMemoryError) {
                continuation.resumeWithException(InterceptedOOMException(e))
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }
    }

    override fun <Result> then(executor: Executor, continuation: Completed<T>.() -> Result): Promise<Result> {
        val pending = Pending<Result>()
        val promise = Promise(pending)

        executor.execute {
            val newState = try {
                Succeeded(continuation())
            } catch (e: OutOfMemoryError) {
                Failed(InterceptedOOMException(e))
            } catch (e: Exception) {
                Failed(e)
            }

            complete(promise, pending, newState)
        }

        return promise
    }

    override fun <Result> then(continuation: Completed<T>.() -> Result): Promise<Result> {
        return try {
            Promise(Succeeded(continuation()))
        } catch (e: OutOfMemoryError) {
            Promise(Failed(InterceptedOOMException(e)))
        } catch (e: Exception) {
            Promise(Failed(e))
        }
    }

    override fun <Result> thenFlatten(executor: Executor, continuation: Completed<T>.() -> Promise<Result>): Promise<Result> {
        val pending = Pending<Result>()
        val promise = Promise(pending)

        executor.execute {
            try {
                val task = continuation()
                task.then(ThenFlattenListener(promise, pending))
            } catch (e: OutOfMemoryError) {
                complete(promise, pending, Failed(InterceptedOOMException(e)))
            } catch (e: Exception) {
                complete(promise, pending, Failed(e))
            }
        }

        return promise
    }

    override fun <Result> thenFlatten(continuation: Completed<T>.() -> Promise<Result>): Promise<Result> {
        val pending = Pending<Result>()
        val promise = Promise(pending)

        try {
            val task = continuation()
            task.then(ThenFlattenListener(promise, pending))
        } catch (e: OutOfMemoryError) {
            complete(promise, pending, Failed(InterceptedOOMException(e)))
        } catch (e: Exception) {
            complete(promise, pending, Failed(e))
        }

        return promise
    }
}
