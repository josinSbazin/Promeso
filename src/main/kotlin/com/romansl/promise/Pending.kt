package com.romansl.promise

import java.util.concurrent.Executor
import kotlin.coroutines.experimental.Continuation

internal class Pending<T>: State<T>() {
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
    override fun <Result> then(promise: Promise<Result>, continuation: Completed<T>.() -> Result, executor: Executor) {
        continuations = Node(continuations) {
            executor.execute {
                val newState = try {
                    Succeeded(it.continuation())
                } catch (e: OutOfMemoryError) {
                    Failed<Result>(InterceptedOOMException(e))
                } catch (e: Exception) {
                    Failed<Result>(e)
                }

                promise.state.getAndSet(newState).complete(newState)
            }
        }
    }

    @Synchronized
    override fun <Result> immediateThen(continuation: Completed<T>.() -> Result): Promise<Result> {
        val promise = Promise<Result>(Pending())
        continuations = Node(continuations) {
            val newState = try {
                Succeeded(it.continuation())
            } catch (e: OutOfMemoryError) {
                Failed<Result>(InterceptedOOMException(e))
            } catch (e: Exception) {
                Failed<Result>(e)
            }

            promise.state.getAndSet(newState).complete(newState)
        }
        return promise
    }

    @Synchronized
    override fun <Result> after(promise: Promise<Result>, continuation: Completed<T>.() -> Promise<Result>, executor: Executor) {
        continuations = Node(continuations) {
            executor.execute {
                try {
                    val task = it.continuation()
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
    }

    @Synchronized
    override fun <Result> immediateAfter(promise: Promise<Result>, continuation: Completed<T>.() -> Promise<Result>) {
        continuations = Node(continuations) {
            try {
                val task = it.continuation()
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
