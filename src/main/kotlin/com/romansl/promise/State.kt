package com.romansl.promise

import java.util.concurrent.Executor
import kotlin.coroutines.experimental.Continuation

abstract class State<out T> {
    internal abstract fun complete(newState: Completed<*>)
    internal abstract fun <Result> then(continuation: Completed<T>.() -> Result): Promise<Result>
    internal abstract fun <Result> then(executor: Executor, continuation: Completed<T>.() -> Result): Promise<Result>
    internal abstract fun <Result> thenFlatten(continuation: Completed<T>.() -> Promise<Result>): Promise<Result>
    internal abstract fun <Result> thenFlatten(executor: Executor, continuation: Completed<T>.() -> Promise<Result>): Promise<Result>

    internal abstract fun thenContinuation(continuation: Continuation<T>)
    internal abstract fun thenContinuation(continuation: Continuation<T>, executor: Executor)
}
