package com.romansl.promise

import java.util.concurrent.CancellationException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@Suppress("UNCHECKED_CAST")
class Promise<out T> internal constructor(initState: State<T>) {
    internal constructor(failed: Failed) : this(failed as State<T>)

    internal val state: AtomicReference<State<*>> = AtomicReference(initState)

    fun <Result> then(continuation: Completed<T>.() -> Result): Promise<Result> {
        return (state.get() as State<T>).then(continuation)
    }

    fun <Result> then(executor: Executor, continuation: Completed<T>.() -> Result): Promise<Result> {
        return (state.get() as State<T>).then(executor, continuation)
    }

    /**
     * Equivalent to: then { ... }.flatten()
     */
    fun <Result> thenFlatten(continuation: Completed<T>.() -> Promise<Result>): Promise<Result> {
        return (state.get() as State<T>).thenFlatten(continuation)
    }

    /**
     * Equivalent to: then(executor) { ... }.flatten()
     */
    fun <Result> thenFlatten(executor: Executor, continuation: Completed<T>.() -> Promise<Result>): Promise<Result> {
        return (state.get() as State<T>).thenFlatten(executor, continuation)
    }

    fun isPending(): Boolean = state.get() is Pending

    fun isFailed(): Boolean = state.get() is Failed

    fun isCancelled(): Boolean {
        val state = state.get()
        return state is Failed && state.exception is CancellationException
    }

    inline fun ifPending(body: () -> Unit): Promise<T> {
        if (isPending()) {
            body()
        }

        return this
    }

    fun getResult(): T = (state.get() as Completed<T>).result

    fun waitForCompletion(): T {
        val latch = CountDownLatch(1)
        then { latch.countDown() }
        latch.await()
        return (state.get() as Completed<T>).result
    }

    fun waitForCompletion(timeout: Long, unit: TimeUnit = TimeUnit.MILLISECONDS): T {
        val latch = CountDownLatch(1)
        then { latch.countDown() }
        latch.await(timeout, unit)
        return (state.get() as Completed<T>).result
    }

    fun cancel() {
        val state = state.get()
        if (state is Pending) {
            complete(this, state, Failed(CancellationException()))
        }
    }

    companion object {
        @Volatile
        @JvmStatic
        var unhandledErrorListener: ((e: Exception) -> Unit)? = null

        @JvmStatic
        fun <Result> create(): Completion<Result> = Completion()

        @JvmStatic
        fun <Result> succeeded(value: Result): Promise<Result> = Promise(Succeeded(value))

        @JvmStatic
        fun <Result> failed(error: Exception): Promise<Result> = Promise(Failed(error))

        @JvmStatic
        fun <Result> cancelled(): Promise<Result> = Promise(Failed(CancellationException()))

        @JvmStatic
        fun <Result> call(callable: () -> Result): Promise<Result> {
            return try {
                succeeded(callable())
            } catch (e: OutOfMemoryError) {
                failed(InterceptedOOMException(e))
            } catch (e: Exception) {
                failed(e)
            }
        }

        @JvmStatic
        fun <Result> callFlatten(callable: () -> Promise<Result>): Promise<Result> {
            return try {
                val tcs = Promise.create<Result>()
                callable().thenComplete(tcs)
                tcs.promise
            } catch (e: OutOfMemoryError) {
                failed(InterceptedOOMException(e))
            } catch (e: Exception) {
                failed(e)
            }
        }

        @JvmStatic
        fun <Result> call(executor: Executor, callable: () -> Result): Promise<Result> {
            val tcs = Promise.create<Result>()
            executor.execute {
                try {
                    tcs.setResult(callable())
                } catch (e: OutOfMemoryError) {
                    tcs.setError(InterceptedOOMException(e))
                } catch (e: Exception) {
                    tcs.setError(e)
                }
            }
            return tcs.promise
        }

        @JvmStatic
        fun <Result> callFlatten(executor: Executor, callable: () -> Promise<Result>): Promise<Result> {
            val tcs = Promise.create<Result>()
            executor.execute {
                try {
                    callable().thenComplete(tcs)
                } catch (e: OutOfMemoryError) {
                    tcs.setError(InterceptedOOMException(e))
                } catch (e: Exception) {
                    tcs.setError(e)
                }
            }
            return tcs.promise
        }

        @JvmStatic
        fun whenAll(promises: Collection<Promise<*>>): Promise<Array<Completed<*>>> {
            return whenAll(*promises.toTypedArray())
        }

        @JvmStatic
        fun whenAll(vararg promises: Promise<*>): Promise<Array<Completed<*>>> {
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
                    states[i] = this

                    if (count.decrementAndGet() == 0) {
                        allFinished.setResult(states as Array<Completed<*>>)
                    }
                }
            }

            return allFinished.promise
        }
    }
}

fun <T> Promise<T>.thenComplete(completion: Completion<T>) {
    @Suppress("UNCHECKED_CAST")
    (state.get() as State<T>).then(ThenFlattenListener(completion.promise, completion.pending))
}

fun <R> Promise<Promise<R>>.flatten(): Promise<R> {
    val completion = Completion<R>()
    then {
        try {
            result.thenComplete(completion)
        } catch (e: OutOfMemoryError) {
            completion.setError(InterceptedOOMException(e))
        } catch (e: Exception) {
            completion.setError(e)
        }
    }
    return completion.promise
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun complete(promise: Promise<*>, pending: Pending<*>, completed: Completed<*>) {
    if (promise.state.compareAndSet(pending, completed)) {
        pending.complete(completed)
    }
}
