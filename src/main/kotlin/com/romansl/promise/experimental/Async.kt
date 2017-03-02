package com.romansl.promise.experimental

import com.romansl.promise.*
import java.lang.Exception
import java.util.concurrent.Executor
import kotlin.coroutines.experimental.*

internal class PromiseContinuation<T>(override val context: CoroutineContext) : Continuation<T> {
    internal val promise: Promise<T> = Promise(Pending())

    override fun resume(value: T) {
        val state = Succeeded(value)
        promise.state.getAndSet(state).complete(state)
    }

    override fun resumeWithException(exception: Throwable) {
        @Suppress("IfThenToElvis")
        val e: Exception = if (exception is Exception) {
            exception
        } else {
            RuntimeException(exception)
        }
        val state = Failed<T>(e)
        promise.state.getAndSet(state).complete(state)
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
