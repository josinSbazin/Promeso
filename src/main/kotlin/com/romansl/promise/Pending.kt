package com.romansl.promise

import java.util.concurrent.Executor
import kotlin.coroutines.experimental.Continuation

internal class Pending<out T>: State<T>() {
    private var continuations: Node<T>? = null

    @Synchronized
    override fun thenContinuation(continuation: Continuation<T>) {
        continuations = Node(continuations) { completed ->
            try {
                continuation.resume(completed.result)
            } catch (e: OutOfMemoryError) {
                continuation.resumeWithException(InterceptedOOMException(e))
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }
    }

    @Synchronized
    override fun thenContinuation(continuation: Continuation<T>, executor: Executor) {
        continuations = Node(continuations) { completed ->
            executor.execute {
                try {
                    continuation.resume(completed.result)
                } catch (e: OutOfMemoryError) {
                    continuation.resumeWithException(InterceptedOOMException(e))
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            }
        }
    }

    @Synchronized
    override fun <Result> then(executor: Executor, continuation: Completed<T>.() -> Result): Promise<Result> {
        val pending = Pending<Result>()
        val promise = Promise<Result>(pending)
        continuations = Node(continuations) {
            executor.execute {
                val newState = try {
                    Succeeded(it.continuation())
                } catch (e: OutOfMemoryError) {
                    Failed(InterceptedOOMException(e))
                } catch (e: Exception) {
                    Failed(e)
                }

                complete(promise, pending, newState)
            }
        }
        return promise
    }

    @Synchronized
    override fun <Result> then(continuation: Completed<T>.() -> Result): Promise<Result> {
        val pending = Pending<Result>()
        val promise = Promise<Result>(pending)
        continuations = Node(continuations) {
            val newState = try {
                Succeeded(it.continuation())
            } catch (e: OutOfMemoryError) {
                Failed(InterceptedOOMException(e))
            } catch (e: Exception) {
                Failed(e)
            }

            complete(promise, pending, newState)
        }
        return promise
    }

    @Synchronized
    override fun <Result> thenFlatten(executor: Executor, continuation: Completed<T>.() -> Promise<Result>): Promise<Result> {
        val pending = Pending<Result>()
        val promise = Promise<Result>(pending)
        continuations = Node(continuations) {
            executor.execute {
                try {
                    val task = it.continuation()
                    task.then(ThenFlattenListener(promise, pending))
                } catch (e: OutOfMemoryError) {
                    complete(promise, pending, Failed(InterceptedOOMException(e)))
                } catch (e: Exception) {
                    complete(promise, pending, Failed(e))
                }
            }
        }
        return promise
    }

    @Synchronized
    override fun <Result> thenFlatten(continuation: Completed<T>.() -> Promise<Result>): Promise<Result> {
        val pending = Pending<Result>()
        val promise = Promise<Result>(pending)
        continuations = Node(continuations) {
            try {
                val task = it.continuation()
                task.then(ThenFlattenListener(promise, pending))
            } catch (e: OutOfMemoryError) {
                complete(promise, pending, Failed(InterceptedOOMException(e)))
            } catch (e: Exception) {
                complete(promise, pending, Failed(e))
            }
        }
        return promise
    }

    @Suppress("UNCHECKED_CAST")
    override fun complete(newState: Completed<*>) {
        var node = synchronized(this) {
            val tmp = continuations
            continuations = null
            tmp
        }

        if (node == null) {
            if (newState is Failed) {
                Promise.unhandledErrorListener?.invoke(newState.exception)
            }
            return
        }

        while (true) {
            val localNode = node ?: break
            localNode.body(newState as Completed<T>)
            node = localNode.next
        }
    }

    private class Node<T>(val next: Node<T>?, val body: (Completed<T>) -> Unit)
}
