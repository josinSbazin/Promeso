# Promeso
Simple promise library for Kotlin

[![](https://jitpack.io/v/romansl/Promeso.svg)](https://jitpack.io/#romansl/Promeso)

Callback to Promise:

```kotlin
    fun someCallbackToPromise(): Promise<Int> {
        val completion = Promise.create<Int>()

        foo.setSomeCallback(object : SomeCallback {
            override fun onCallback(bar: Int) {
                completion.setResult(bar)
            }

            override fun onError(e: Exception) {
                completion.setError(e)
            }
        })

        return completion.promise
    }
```

Simple usage:

```kotlin
    fun processPromise() {
        someCallbackToPromise().then(diskExecutor) { 
            readSomeDataFromDiskByIndex(result)
        }.then(uiExecutor) {
            try {
                displayResult(result)
            } catch (e: Exception) {
                log.error("some error", e)
            }
        }
    }
```
