package com.romansl.promise

import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicReference

public class Pending<T>: State<T>() {
    override val result: T
        get() = throw IllegalStateException()
    private val continuations = AtomicReference<Node<T>?>();

    override fun <Result> then(promise: Promise<Result>, continuation: Completed<T>.() -> Result, executor: Executor) {
        add {
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

    override fun <Result> immediateThen(promise: Promise<Result>, continuation: Completed<T>.() -> Result) {
        add {
            try {
                val newState = Succeeded(it.continuation())
                promise.state.getAndSet(newState).complete(newState)
            } catch (e: Exception) {
                val newState = Failed<Result>(e)
                promise.state.getAndSet(newState).complete(newState)
            }
        }
    }

    override fun <Result> after(promise: Promise<Result>, continuation: Completed<T>.() -> Promise<Result>, executor: Executor) {
        add {
            println("P-after-add")
            executor.execute {
                println("P-after-execute")
                try {
                    println("P-after-try")
                    val task = it.continuation()
                    task.then {
                        println("P-after-then")
                        promise.state.getAndSet(this).complete(this)
                    }
                } catch (e: Exception) {
                    println("P-after-catch")
                    val newState = Failed<Result>(e)
                    promise.state.getAndSet(newState).complete(newState)
                }
            }
        }
    }

    override fun <Result> immediateAfter(promise: Promise<Result>, continuation: Completed<T>.() -> Promise<Result>) {
        add {
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
    private fun add(item: (Completed<T>) -> Unit) {
        val newHead = Node<T>(null, item);
        var oldHead: Node<T>?;
        val head = continuations
        do {
            oldHead = head.get();
            newHead.next = oldHead;
        } while (!head.compareAndSet(oldHead, newHead));
    }

    private class Node<T>(var next: Node<T>?, val body: (Completed<T>) -> Unit)
}
