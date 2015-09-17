package com.romansl.promise

import java.util.concurrent.Executor

internal class Pending<T>: State<T>() {
    private var continuations: Node<T>? = null

    @Synchronized
    override fun <Result> then(promise: Promise<Result>, continuation: Completed<T>.() -> Result, executor: Executor) {
        continuations = Node(continuations) {
            executor.execute {
                try {
                    val newState = Succeeded(it.continuation())
                    promise.state.getAndSet(newState).complete(newState)
                } catch (e: Exception) {
                    val newState = Failed<Result>(e)
                    promise.state.getAndSet(newState).complete(newState)
                }
            }
        }
    }

    @Synchronized
    override fun <Result> immediateThen(continuation: Completed<T>.() -> Result): Promise<Result> {
        val promise = Promise<Result>(Pending())
        continuations = Node(continuations) {
            try {
                val newState = Succeeded(it.continuation())
                promise.state.getAndSet(newState).complete(newState)
            } catch (e: Exception) {
                val newState = Failed<Result>(e)
                promise.state.getAndSet(newState).complete(newState)
            }
        }
        return promise
    }

    @Synchronized
    override fun <Result> after(promise: Promise<Result>, continuation: Completed<T>.() -> Promise<Result>, executor: Executor) {
        continuations = Node(continuations) {
            executor.execute {
                try {
                    val task = it.continuation()
                    task.then {
                        promise.state.getAndSet(this).complete(this)
                    }
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
                task.then {
                    promise.state.getAndSet(this).complete(this)
                }
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

        while (true) {
            val localNode = node
            if (localNode == null)
                break
            localNode.body(newState as Completed<T>)
            node = localNode.next
        }
    }

    private class Node<T>(val next: Node<T>?, val body: (Completed<T>) -> Unit)
}
