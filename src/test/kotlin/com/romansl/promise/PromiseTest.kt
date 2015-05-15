package com.romansl.promise

import junit.framework.TestCase
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import kotlin.test.assertEquals
import kotlin.test.assertTrue

public class PromiseTest: TestCase() {

    private fun assertThrows(expected: Throwable?, block: () -> Unit) {
        try {
            block()
        } catch (e: Throwable) {
            assertEquals(expected, e)
            return
        }

        assertTrue(false, "Block not throwing exception")
    }

    public fun testSucceeded() {
        val promise = Promise.succeeded(10)
        assertTrue(promise.state.get() is Succeeded<*>)
        assertEquals(10, promise.state.get().result)
    }

    public fun testFailed() {
        val exception = RuntimeException("hello")
        val promise = Promise.failed<Int>(exception)
        assertTrue(promise.state.get() is Failed<*>)
        assertThrows(exception) {
            promise.state.get().result
        }
    }

    public fun testThenSynchronousSucceeded() {
        val promise = Promise.succeeded(10)
        promise.then {
            assertEquals(10, result)
            "hello"
        }.then {
            assertEquals("hello", result)
        }

        val completion = Promise.create<Int>()
        completion.promise.then {
            assertEquals(10, result)
            "hello"
        }.then {
            assertEquals("hello", result)
        }
        completion.result = 10
    }

    public fun testThenSynchronousFailed() {
        val exception = RuntimeException("world")
        val promise = Promise.failed<Int>(exception)
        promise.then {
            assertThrows(exception) {
                result
            }
            result
        }.then {
            assertThrows(exception) {
                result
            }
        }

        val completion = Promise.create<Int>()
        completion.promise.then {
            assertThrows(exception) {
                result
            }
            result
        }.then {
            assertThrows(exception) {
                result
            }
        }
        completion.error = exception
    }

    public fun testAfterSynchronous() {
        val exception = RuntimeException("error")
        val promise = Promise.succeeded(10)
        promise.after {
            val r = result
            assertEquals(10, r)
            Promise.succeeded(r + 11)
        }.after<Int> {
            assertEquals(21, result)
            throw exception
        }.then {
            assertThrows(exception) {
                result
            }
        }

        val completion = Promise.create<Int>()
        completion.promise.after {
            val r = result
            assertEquals(10, r)
            Promise.succeeded(r + 11)
        }.after<Int> {
            assertEquals(21, result)
            throw exception
        }.then {
            assertThrows(exception) {
                result
            }
        }

        completion.result = 10
    }

    public fun testAfterAsynchronous1() {
        val exception = RuntimeException("error")
        val latch = CountDownLatch(1)
        val executor = Executors.newCachedThreadPool()

        val promise = Promise.succeeded(10)
        promise.after(executor) {
            //println("1")
            Thread.sleep(0)
            val r = result
            assertEquals(10, r)
            Promise.succeeded(r + 11)
        }.after<Int>(executor) {
            //println("2")
            assertEquals(21, result)
            throw exception
        }.then(executor) {
            println("3")
            assertThrows(exception) {
                result
            }
        }.then {
            println("4")
            latch.countDown()
        }

        latch.await()
        executor.shutdown()
    }

    public fun testAfterAsynchronous2() {
        val exception = RuntimeException("error")
        val latch = CountDownLatch(1)
        val executor = Executors.newCachedThreadPool()

        val completion = Promise.create<Int>()
        completion.promise.after(executor) {
            val r = result
            assertEquals(10, r)
            Promise.succeeded(r + 11)
        }.after<Int>(executor) {
            assertEquals(21, result)
            throw exception
        }.then(executor) {
            assertThrows(exception) {
                result
            }
        }.then {
            latch.countDown()
        }
        completion.result = 10

        latch.await()
        executor.shutdownNow()
    }
}
