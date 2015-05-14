package com.romansl.promise

import junit.framework.TestCase
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
        }

        val completion = Promise.create<Int>()
        completion.promise.then {
            assertEquals(10, result)
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
        }

        val completion = Promise.create<Int>()
        completion.promise.then {
            assertThrows(exception) {
                result
            }
        }
        completion.error = exception
    }
}
