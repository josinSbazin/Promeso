package com.romansl.promise

import java.util.concurrent.Executor

public abstract class State<T> {
    public abstract val result: T
    internal abstract fun moveToState(newState: State<T>)
    internal abstract fun <Result> then(tcs: Completion<Result>, continuation: State<T>.() -> Result, executor: Executor)
    internal abstract fun <Result> after(tcs: Completion<Result>, continuation: State<T>.() -> Promise<Result>, executor: Executor)
    internal abstract fun <Result> immediateThen(tcs: Completion<Result>, continuation: State<T>.() -> Result)
    internal abstract fun <Result> immediateAfter(tcs: Completion<Result>, continuation: State<T>.() -> Promise<Result>)
}
