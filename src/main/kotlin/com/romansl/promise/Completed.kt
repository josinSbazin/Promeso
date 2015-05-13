package com.romansl.promise

import java.util.concurrent.Executor

public abstract class Completed<T> : State<T>() {
    override fun moveToState(newState: State<T>) {
        throw IllegalStateException()
    }

    override fun <Result> then(tcs: Completion<Result>, continuation: State<T>.() -> Result, executor: Executor) {
        executor.execute {
            try {
                tcs.state = Succeeded(continuation())
            } catch (e: Exception) {
                tcs.state = Failed(e)
            }
        }
    }

    override fun <Result> immediateThen(tcs: Completion<Result>, continuation: State<T>.() -> Result) {
        try {
            tcs.state = Succeeded(continuation())
        } catch (e: Exception) {
            tcs.state = Failed(e)
        }
    }

    override fun <Result> after(tcs: Completion<Result>, continuation: State<T>.() -> Promise<Result>, executor: Executor) {
        executor.execute {
            try {
                val task = continuation()
                task.then {
                    tcs.state = this
                }
            } catch (e: Exception) {
                tcs.state = Failed(e)
            }
        }
    }

    override fun <Result> immediateAfter(tcs: Completion<Result>, continuation: State<T>.() -> Promise<Result>) {
        try {
            val task = continuation()
            task.then {
                tcs.state = this
            }
        } catch (e: Exception) {
            tcs.state = Failed(e)
        }
    }
}
