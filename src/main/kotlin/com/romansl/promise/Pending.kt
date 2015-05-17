package com.romansl.promise

import java.util.concurrent.Executor

public class Pending<T>: State<T>() {
    private val lock = Object()
    private var continuations: Node<T>? = null

    override fun <Result> then(promise: Promise<Result>, continuation: Completed<T>.() -> Result, executor: Executor) = synchronized(lock) {
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

    override fun <Result> immediateThen(promise: Promise<Result>, continuation: Completed<T>.() -> Result) = synchronized(lock) {
        continuations = Node(continuations) {
            try {
                val newState = Succeeded(it.continuation())
                promise.state.getAndSet(newState).complete(newState)
            } catch (e: Exception) {
                val newState = Failed<Result>(e)
                promise.state.getAndSet(newState).complete(newState)
            }
        }
    }

    override fun <Result> after(promise: Promise<Result>, continuation: Completed<T>.() -> Promise<Result>, executor: Executor) = synchronized(lock) {
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

    override fun <Result> immediateAfter(promise: Promise<Result>, continuation: Completed<T>.() -> Promise<Result>) = synchronized(lock) {
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

    override fun complete(newState: Completed<T>) {
        var node = synchronized(lock) {
            val tmp = continuations
            continuations = null
            tmp
        }

        while (true) {
            val localNode = node
            if (localNode == null)
                break
            localNode.body(newState)
            node = localNode.next
        }
    }

    private class Node<T>(val next: Node<T>?, val body: (Completed<T>) -> Unit)
}
