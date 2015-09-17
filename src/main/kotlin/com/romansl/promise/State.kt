package com.romansl.promise

import java.util.concurrent.Executor

internal abstract class State<out T> {
    internal abstract fun complete(newState: Completed<*>)
    internal abstract fun <Result> then(promise: Promise<Result>, continuation: Completed<T>.() -> Result, executor: Executor)
    internal abstract fun <Result> after(promise: Promise<Result>, continuation: Completed<T>.() -> Promise<Result>, executor: Executor)
    internal abstract fun <Result> immediateThen(continuation: Completed<T>.() -> Result): Promise<Result>
    internal abstract fun <Result> immediateAfter(promise: Promise<Result>, continuation: Completed<T>.() -> Promise<Result>)
}
