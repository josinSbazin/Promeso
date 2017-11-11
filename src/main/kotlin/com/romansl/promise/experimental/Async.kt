package com.romansl.promise.experimental

import com.romansl.promise.*
import java.lang.Exception
import java.util.concurrent.Executor
import kotlin.coroutines.experimental.*

internal class PromiseContinuation<T>(override val context: CoroutineContext) : Continuation<T> {
    private val pending = Pending<T>()
    internal val promise: Promise<T> = Promise(pending)

    override fun resume(value: T) {
        complete(promise, pending, Succeeded(value))
    }

    override fun resumeWithException(exception: Throwable) {
        @Suppress("IfThenToElvis")
        val e: Exception = if (exception is Exception) {
            exception
        } else {
            RuntimeException(exception)
        }
        complete(promise, pending, Failed(e))
    }
}

@Suppress("unused")
fun <T> Promise.Companion.async(context: CoroutineContext = EmptyCoroutineContext, block: suspend () -> T): Promise<T> {
    val continuation = PromiseContinuation<T>(context)
    block.startCoroutine(continuation)
    return continuation.promise
}

suspend fun <T> Promise<T>.await(): T = suspendCoroutine {
    @Suppress("UNCHECKED_CAST")
    (state.get() as State<T>).thenContinuation(it)
}

suspend fun <T> Promise<T>.await(executor: Executor): T = suspendCoroutine {
    @Suppress("UNCHECKED_CAST")
    (state.get() as State<T>).thenContinuation(it, executor)
}
