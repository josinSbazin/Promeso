package com.romansl.promise

import junit.framework.TestCase
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

public class PromiseTest: TestCase() {

    private fun assertThrows(expected: Throwable?, block: () -> Unit) {
        try {
            block()
        } catch (e: Throwable) {
            assertEquals(expected, e)
            return
        }

        assertTrue("Block not throwing exception", false)
    }

    public fun testSucceeded() {
        val promise = Promise.succeeded(10)
        assertTrue(promise.state.get() is Succeeded<*>)
        assertEquals(10, (promise.state.get() as Completed).result)
    }

    public fun testFailed() {
        val exception = RuntimeException("hello")
        val promise = Promise.failed<Int>(exception)
        assertTrue(promise.state.get() is Failed<*>)
        assertThrows(exception) {
            (promise.state.get() as Completed).result
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
        completion.setResult(10)
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
        completion.setError(exception)
    }

    public fun testAfterSynchronous() {
        val exception = RuntimeException("error")
        val promise = Promise.succeeded(10)
        promise.thenFlatten {
            val r = result
            assertEquals(10, r)
            Promise.succeeded(r + 11)
        }.thenFlatten<Int> {
            assertEquals(21, result)
            throw exception
        }.then {
            assertThrows(exception) {
                result
            }
        }

        val completion = Promise.create<Int>()
        completion.promise.thenFlatten {
            val r = result
            assertEquals(10, r)
            Promise.succeeded(r + 11)
        }.thenFlatten<Int> {
            assertEquals(21, result)
            throw exception
        }.then {
            assertThrows(exception) {
                result
            }
        }

        completion.setResult(10)
    }

    public fun testAfterAsynchronous1() {
        val exception = RuntimeException("error")
        val latch = CountDownLatch(1)
        val executor = Executors.newCachedThreadPool()

        val promise = Promise.succeeded(10)
        promise.thenFlatten(executor) {
            val r = result
            assertEquals(10, r)
            Promise.succeeded(r + 11)
        }.thenFlatten<Int>(executor) {
            assertEquals(21, result)
            throw exception
        }.then(executor) {
            assertThrows(exception) {
                result
            }
        }.then {
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
        completion.promise.thenFlatten(executor) {
            val r = result
            assertEquals(10, r)
            Promise.succeeded(r + 11)
        }.thenFlatten<Int>(executor) {
            assertEquals(21, result)
            throw exception
        }.then(executor) {
            assertThrows(exception) {
                result
            }
        }.then {
            latch.countDown()
        }
        completion.setResult(10)

        latch.await()
        executor.shutdownNow()
    }

    fun testThenCompleteSuccess() {
        val completion1 = Promise.create<Number>()
        val completion2 = Promise.create<Int>()
        val out = ArrayList<Number>()

        val promise2 = completion2.promise
        promise2.then {
            out.add(result)
        }

        assertEquals("[]", out.toString())

        promise2.thenComplete(completion1)

        assertEquals("[]", out.toString())

        completion2.setResult(10)

        assertEquals("[10]", out.toString())
    }

    fun testThenCompleteFail() {
        val completion1 = Promise.create<Number>()
        val completion2 = Promise.create<Int>()
        val out = ArrayList<String?>()

        val promise2 = completion2.promise
        promise2.then {
            try {
                out.add(result.toString())
            } catch (e: Exception) {
                out.add(e.message)
            }
        }

        assertEquals("[]", out.toString())

        promise2.thenComplete(completion1)

        assertEquals("[]", out.toString())

        completion2.setError(RuntimeException("Hello World"))

        assertEquals("[Hello World]", out.toString())
    }
}
