package com.romansl.promise

import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicReference
import kotlin.platform.platformStatic

public class Promise<T> private(initState: State<T>) {
    internal val state = AtomicReference<State<T>>()

    init {
        state.set(initState)
    }

    public fun <Result> then(continuation: Completed<T>.() -> Result): Promise<Result> {
        val promise = Promise<Result>(Pending())
        state.get().immediateThen(promise, continuation)
        return promise
    }

    public fun <Result> then(executor: Executor, continuation: Completed<T>.() -> Result): Promise<Result> {
        val promise = Promise<Result>(Pending())
        state.get().then(promise, continuation, executor)
        return promise
    }

    public fun <Result> after(continuation: Completed<T>.() -> Promise<Result>): Promise<Result> {
        val promise = Promise<Result>(Pending())
        state.get().immediateAfter(promise, continuation)
        return promise
    }

    public fun <Result> after(executor: Executor, continuation: Completed<T>.() -> Promise<Result>): Promise<Result> {
        val promise = Promise<Result>(Pending())
        state.get().after(promise, continuation, executor)
        return promise
    }

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
                    tcs.result = callable()
                } catch (e: Exception) {
                    tcs.error = e
                }
            }
            return tcs.promise
        }
    }
}
