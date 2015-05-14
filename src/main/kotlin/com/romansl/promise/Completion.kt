package com.romansl.promise

public class Completion<Result> internal(public val promise: Promise<Result>) {
    public var result: Result
        get() = throw AssertionError("Write-only property")
        set(src) {
            val state = Succeeded(src)
            promise.state.getAndSet(state).moveToState(state)
        }
    public var error: Exception
        get() = throw AssertionError("Write-only property")
        set(src) {
            val state = Failed<Result>(src)
            promise.state.getAndSet(state).moveToState(state)
        }
}
