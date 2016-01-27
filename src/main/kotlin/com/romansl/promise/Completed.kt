package com.romansl.promise

import java.util.concurrent.Executor

public abstract class Completed<out T> : State<T>() {
    public abstract val result: T

    override fun complete(newState: Completed<*>) = throw AssertionError()

    override fun <Result> then(promise: Promise<Result>, continuation: Completed<T>.() -> Result, executor: Executor) {
        executor.execute {
            val newState = try {
                Succeeded(continuation())
            } catch (e: OutOfMemoryError) {
                Failed<Result>(InterceptedOOMException(e))
            } catch (e: Exception) {
                Failed<Result>(e)
            }

            promise.state.getAndSet(newState).complete(newState)
        }
    }

    override fun <Result> immediateThen(continuation: Completed<T>.() -> Result): Promise<Result> {
        return try {
            Promise(Succeeded(continuation()))
        } catch (e: OutOfMemoryError) {
            Promise(Failed(InterceptedOOMException(e)))
        } catch (e: Exception) {
            Promise(Failed(e))
        }
    }

    override fun <Result> after(promise: Promise<Result>, continuation: Completed<T>.() -> Promise<Result>, executor: Executor) {
        executor.execute {
            try {
                val task = continuation()
                task.then(ThenFlattenListener(promise))
            } catch (e: OutOfMemoryError) {
                val newState = Failed<Result>(InterceptedOOMException(e))
                promise.state.getAndSet(newState).complete(newState)
            } catch (e: Exception) {
                val newState = Failed<Result>(e)
                promise.state.getAndSet(newState).complete(newState)
            }
        }
    }

    override fun <Result> immediateAfter(promise: Promise<Result>, continuation: Completed<T>.() -> Promise<Result>) {
        try {
            val task = continuation()
            task.then(ThenFlattenListener(promise))
        } catch (e: OutOfMemoryError) {
            val newState = Failed<Result>(InterceptedOOMException(e))
            promise.state.getAndSet(newState).complete(newState)
        } catch (e: Exception) {
            val newState = Failed<Result>(e)
            promise.state.getAndSet(newState).complete(newState)
        }
    }
}
