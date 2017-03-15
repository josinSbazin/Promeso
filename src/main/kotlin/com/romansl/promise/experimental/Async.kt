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

class Selector<R> {
    internal val completion = Promise.create<R>()

    fun <T> case(promise: Promise<T>, body: Completed<T>.() -> R) {
        promise.then(body).thenCompleteSafe(completion)
    }

    fun <T> caseFlatten(promise: Promise<T>, body: Completed<T>.() -> Promise<R>) {
        promise.thenFlatten(body).thenCompleteSafe(completion)
    }

    fun <T> case(promise: Promise<T>, executor: Executor, body: Completed<T>.() -> R) {
        promise.then(executor, body).thenCompleteSafe(completion)
    }

    fun <T> caseFlatten(promise: Promise<T>, executor: Executor, body: Completed<T>.() -> Promise<R>) {
        promise.thenFlatten(executor, body).thenCompleteSafe(completion)
    }

    fun case(promise: Promise<R>) {
        promise.thenCompleteSafe(completion)
    }
}

suspend fun <R> select(body: Selector<R>.() -> Unit): R {
    val selector = Selector<R>()
    selector.body()
    return selector.completion.promise.await()
}

suspend fun foo() {
    val p1 = Promise.succeeded(0)
    val p2 = Promise.succeeded(0)

    select<Int> {
        case(p1) {
            result + 1
        }
        case(p2) {
            result + 2
        }
        case(p2)
    }
}
