package com.romansl.promise

import java.util.concurrent.Executor

public abstract class Completed<T> : State<T>() {
    override fun complete(newState: Completed<T>) {
        throw IllegalStateException()
    }

    override fun <Result> then(promise: Promise<Result>, continuation: Completed<T>.() -> Result, executor: Executor) {
        executor.execute {
            try {
                val newState = Succeeded(continuation())
                promise.state.getAndSet(newState).complete(newState)
            } catch (e: Exception) {
                val newState = Failed<Result>(e)
                promise.state.getAndSet(newState).complete(newState)
            }
        }
    }

    override fun <Result> immediateThen(promise: Promise<Result>, continuation: Completed<T>.() -> Result) {
        try {
            val newState = Succeeded(continuation())
            promise.state.getAndSet(newState).complete(newState)
        } catch (e: Exception) {
            val newState = Failed<Result>(e)
            promise.state.getAndSet(newState).complete(newState)
        }
    }

    override fun <Result> after(promise: Promise<Result>, continuation: Completed<T>.() -> Promise<Result>, executor: Executor) {
        executor.execute {
            try {
                val task = continuation()
                task.then {
                    promise.state.getAndSet(this).complete(this)
                }
            } catch (e: Exception) {
                val newState = Failed<Result>(e)
                promise.state.getAndSet(newState).complete(newState)
            }
        }
    }

    override fun <Result> immediateAfter(promise: Promise<Result>, continuation: Completed<T>.() -> Promise<Result>) {
        try {
            val task = continuation()
            task.then {
                promise.state.getAndSet(this).complete(this)
            }
        } catch (e: Exception) {
            val newState = Failed<Result>(e)
            promise.state.getAndSet(newState).complete(newState)
        }
    }
}
