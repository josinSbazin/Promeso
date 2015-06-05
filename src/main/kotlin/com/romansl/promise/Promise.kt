package com.romansl.promise

import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicReference
import kotlin.platform.platformStatic

public class Promise<out T> internal constructor(initState: State<T>) {
    internal val state: AtomicReference<State<Any>> = AtomicReference(initState)

    public fun <Result> then(continuation: Completed<T>.() -> Result): Promise<Result> {
        return (state.get() as State<T>).immediateThen(continuation)
    }

    public fun <Result> then(executor: Executor, continuation: Completed<T>.() -> Result): Promise<Result> {
        val promise = Promise<Result>(Pending())
        (state.get() as State<T>).then(promise, continuation, executor)
        return promise
    }

    public fun <Result> after(continuation: Completed<T>.() -> Promise<Result>): Promise<Result> {
        val promise = Promise<Result>(Pending())
        (state.get() as State<T>).immediateAfter(promise, continuation)
        return promise
    }

    public fun <Result> after(executor: Executor, continuation: Completed<T>.() -> Promise<Result>): Promise<Result> {
        val promise = Promise<Result>(Pending())
        (state.get() as State<T>).after(promise, continuation, executor)
        return promise
    }

    public fun isPending(): Boolean = state.get() is Pending

    inline
    public fun isPending(body: () -> Unit): Promise<T> {
        if (isPending()) {
            body()
        }

        return this
    }

    public fun getResult(): T = (state.get() as Completed<T>).result

    companion object {
        platformStatic
        public fun <Result> create(): Completion<Result> = Completion(Promise(Pending()))

        platformStatic
        public fun <Result> succeeded(value: Result): Promise<Result> = Promise(Succeeded(value))

        platformStatic
        public fun <Result> failed(error: Exception): Promise<Result> = Promise(Failed(error))

        platformStatic
        public fun <Result> call(executor: Executor, callable: () -> Result): Promise<Result> {
            val tcs = Promise.create<Result>()
            executor.execute {
                try {
                    tcs.setResult(callable())
                } catch (e: Exception) {
                    tcs.setError(e)
                }
            }
            return tcs.promise
        }
    }
}
