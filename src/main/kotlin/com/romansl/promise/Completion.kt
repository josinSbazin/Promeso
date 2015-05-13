package com.romansl.promise

public class Completion<Result>(public val task: Promise<Result>) {
    public var state: State<Result>
        get() = task.state.get()
        set(src) {
            task.state.getAndSet(src).moveToState(src)
        }
    public var result: Result
        get() = throw AssertionError()
        set(src) {
            val state = Succeeded(src)
            task.state.getAndSet(state).moveToState(state)
        }
    public var error: Exception
        get() = throw AssertionError()
        set(src) {
            val state = Failed<Result>(src)
            task.state.getAndSet(state).moveToState(state)
        }
}
