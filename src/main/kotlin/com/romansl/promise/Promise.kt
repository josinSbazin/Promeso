package com.romansl.promise

import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@Suppress("UNCHECKED_CAST")
public class Promise<out T> internal constructor(initState: State<T>) {
    internal val state: AtomicReference<State<*>> = AtomicReference(initState)

    public fun <Result> then(continuation: Completed<T>.() -> Result): Promise<Result> {
        return (state.get() as State<T>).immediateThen(continuation)
    }

    public fun <Result> then(executor: Executor, continuation: Completed<T>.() -> Result): Promise<Result> {
        val promise = Promise<Result>(Pending())
        (state.get() as State<T>).then(promise, continuation, executor)
        return promise
    }

    /**
     * Equivalent to: then { ... }.flatten()
     */
    public fun <Result> thenFlatten(continuation: Completed<T>.() -> Promise<Result>): Promise<Result> {
        val promise = Promise<Result>(Pending())
        (state.get() as State<T>).immediateAfter(promise, continuation)
        return promise
    }

    /**
     * Equivalent to: then(executor) { ... }.flatten()
     */
    public fun <Result> thenFlatten(executor: Executor, continuation: Completed<T>.() -> Promise<Result>): Promise<Result> {
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

    public inline fun whenPending(body: () -> Unit) {
        if (isPending()) {
            body()
        }
    }

    companion object {
        @JvmStatic
        public fun <Result> create(): Completion<Result> = Completion(Promise(Pending()))

        @JvmStatic
        public fun <Result> succeeded(value: Result): Promise<Result> = Promise(Succeeded(value))

        @JvmStatic
        public fun <Result> failed(error: Exception): Promise<Result> = Promise(Failed(error))

        @JvmStatic
        public fun <Result> call(callable: () -> Result): Promise<Result> {
            return try {
                succeeded(callable())
            } catch (e: Exception) {
                failed(e)
            }
        }

        @JvmStatic
        public fun <Result> callFlatten(callable: () -> Promise<Result>): Promise<Result> {
            return try {
                val tcs = Promise.create<Result>()
                callable().thenComplete(tcs)
                return tcs.promise
            } catch (e: Exception) {
                failed(e)
            }
        }

        @JvmStatic
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

        @JvmStatic
        public fun <Result> callFlatten(executor: Executor, callable: () -> Promise<Result>): Promise<Result> {
            val tcs = Promise.create<Result>()
            executor.execute {
                try {
                    callable().thenComplete(tcs)
                } catch (e: Exception) {
                    tcs.setError(e)
                }
            }
            return tcs.promise
        }

        @JvmStatic
        public fun whenAll(promises: Collection<Promise<*>>): Promise<Array<Completed<*>>> {
            return whenAll(*promises.toTypedArray())
        }

        @JvmStatic
        public fun whenAll(vararg promises: Promise<*>): Promise<Array<Completed<*>>> {
            if (promises.isEmpty()) {
                return succeeded(emptyArray())
            } else if (promises.size == 1) {
                return promises[0].then {
                    arrayOf(this)
                }
            }

            val allFinished = create<Array<Completed<*>>>()
            val states = arrayOfNulls<Completed<*>>(promises.size)
            val count = AtomicInteger(promises.size)

            promises.forEachIndexed { i, promise ->
                promise.then {
                    states.set(i, this)

                    if (count.decrementAndGet() == 0) {
                        @Suppress("CAST_NEVER_SUCCEEDS")
                        allFinished.setResult(states as Array<Completed<*>>)
                    }
                }
            }

            return allFinished.promise
        }
    }
}

class ThenCompleteListener<T>(private val completion: Completion<T>) : (Completed<T>) -> Unit {
    override fun invoke(completed: Completed<T>): Unit {
        completion.promise.state.getAndSet(completed).complete(completed)
    }
}

@Suppress("NOTHING_TO_INLINE")
public inline fun <R> Promise<R>.thenComplete(completion: Completion<R>) {
    then(ThenCompleteListener(completion))
}

public fun <R> Promise<Promise<R>>.flatten(): Promise<R> {
    val completion = Completion(Promise(Pending<R>()))
    then {
        try {
            result.thenComplete(completion)
        } catch (e: Exception) {
            completion.setError(e)
        }
    }
    return completion.promise
}
