package com.romansl.promise

import java.util.concurrent.Executor

public abstract class State<T> {
    public abstract val result: T
    internal abstract fun complete(newState: Completed<T>)
    internal abstract fun <Result> then(promise: Promise<Result>, continuation: Completed<T>.() -> Result, executor: Executor)
    internal abstract fun <Result> after(promise: Promise<Result>, continuation: Completed<T>.() -> Promise<Result>, executor: Executor)
    internal abstract fun <Result> immediateThen(promise: Promise<Result>, continuation: Completed<T>.() -> Result)
    internal abstract fun <Result> immediateAfter(promise: Promise<Result>, continuation: Completed<T>.() -> Promise<Result>)
}
