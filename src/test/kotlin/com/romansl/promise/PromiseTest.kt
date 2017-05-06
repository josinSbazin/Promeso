package com.romansl.promise

import com.romansl.promise.experimental.async
import com.romansl.promise.experimental.await
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class PromiseTest {

    private fun assertThrows(expected: Throwable?, block: () -> Unit) {
        try {
            block()
        } catch (e: Throwable) {
            assertEquals(expected, e)
            return
        }

        assertTrue("Block not throwing exception", false)
    }

    @Test
    fun testSucceeded() {
        val promise = Promise.succeeded(10)
        assertTrue(promise.state.get() is Succeeded<*>)
        assertEquals(10, (promise.state.get() as Completed).result)
    }

    @Test
    fun testFailed() {
        val exception = RuntimeException("hello")
        val promise = Promise.failed<Int>(exception)
        assertTrue(promise.state.get() is Failed<*>)
        assertThrows(exception) {
            (promise.state.get() as Completed).result
        }
    }

    @Test
    fun testThenSynchronousSucceeded() {
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

    @Test
    fun testThenSynchronousFailed() {
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

    @Test
    fun testAfterSynchronous() {
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

    @Test
    fun testAfterAsynchronous1() {
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

    @Test
    fun testAfterAsynchronous2() {
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

    @Test
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

    @Test
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

    @Test
    fun testAsync() {
        val promise = Promise.async<Int> { 10 }
        assertEquals(10, promise.getResult())
    }

    @Test
    fun testAsyncException() {
        val exception = RuntimeException("error")
        val promise = Promise.async<Int> { throw exception }
        assertThrows(exception) {
            promise.getResult()
        }
    }

    @Test
    fun testAwait() {
        val promise = Promise.async<Int> {
            Promise.succeeded(10).await()
        }
        assertEquals(10, promise.getResult())
    }

    @Test
    fun testAwaitException() {
        val exception = RuntimeException("error")
        val promise = Promise.async<Int> {
            Promise.failed<Int>(exception).await()
        }
        assertThrows(exception) {
            promise.getResult()
        }
    }

    @Test
    fun testAwaitExecutor() {
        val executor = TestExecutor()
        val promise = Promise.async<Int> {
            Promise.succeeded(10).await(executor)
        }
        assertTrue(promise.state.get() is Pending<*>)
        executor.run()
        assertEquals(10, promise.getResult())
    }

    @Test
    fun testAwaitExceptionExecutor() {
        val executor = TestExecutor()
        val exception = RuntimeException("error")
        val promise = Promise.async<Int> {
            Promise.failed<Int>(exception).await(executor)
        }
        assertTrue(promise.state.get() is Pending<*>)
        executor.run()
        assertThrows(exception) {
            promise.getResult()
        }
    }

    private class TestExecutor : Executor {
        var runnable: Runnable? = null

        override fun execute(r: Runnable) {
            runnable = r
        }

        fun run() {
            runnable?.run()
        }
    }
}
