package com.romansl.promise

import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicReference

public class Pending<T>: State<T>() {
    override val result: T
        get() = throw IllegalStateException()
    private val continuations = AtomicReference<Node<T>?>();

    override fun <Result> then(promise: Promise<Result>, continuation: State<T>.() -> Result, executor: Executor) {
        add {
            executor.execute {
                try {
                    val newState = Succeeded(it.continuation())
                    promise.state.getAndSet(newState).moveToState(newState)
                } catch (e: Exception) {
                    val newState = Failed<Result>(e)
                    promise.state.getAndSet(newState).moveToState(newState)
                }
            }
        }
    }

    override fun <Result> immediateThen(promise: Promise<Result>, continuation: State<T>.() -> Result) {
        add {
            try {
                val newState = Succeeded(it.continuation())
                promise.state.getAndSet(newState).moveToState(newState)
            } catch (e: Exception) {
                val newState = Failed<Result>(e)
                promise.state.getAndSet(newState).moveToState(newState)
            }
        }
    }

    override fun <Result> after(promise: Promise<Result>, continuation: State<T>.() -> Promise<Result>, executor: Executor) {
        add {
            executor.execute {
                try {
                    val task = it.continuation()
                    task.then {
                        promise.state.getAndSet(this).moveToState(this)
                    }
                } catch (e: Exception) {
                    val newState = Failed<Result>(e)
                    promise.state.getAndSet(newState).moveToState(newState)
                }
            }
        }
    }

    override fun <Result> immediateAfter(promise: Promise<Result>, continuation: State<T>.() -> Promise<Result>) {
        add {
            try {
                val task = it.continuation()
                task.then {
                    promise.state.getAndSet(this).moveToState(this)
                }
            } catch (e: Exception) {
                val newState = Failed<Result>(e)
                promise.state.getAndSet(newState).moveToState(newState)
            }
        }
    }

    override fun moveToState(newState: State<T>) {
        var node = continuations.getAndSet(null)
        while (true) {
            val localNode = node
            if (localNode == null)
                break
            localNode.body(newState)
            node = localNode.next
        }
    }

    // A nonblocking stack.
    private fun add(item: (State<T>) -> Unit) {
        val newHead = Node<T>(null, item);
        var oldHead: Node<T>?;
        val head = continuations
        do {
            oldHead = head.get();
            newHead.next = oldHead;
        } while (!head.compareAndSet(oldHead, newHead));
    }

    private class Node<T>(var next: Node<T>?, val body: (State<T>) -> Unit)
}
