# Core Types Used

The core of the library is very simple structure:

```kotlin
sealed class Result<out E, out T> {
    
    data class Success<T>(val value: T) : Result<Nothing, T>() {
        override fun get(): T = value
        override fun toString(): String = "${Success::class.simpleName}($value)"
    }
    
    data class Failure<E>(val error: E) : Result<E, Nothing>() {
        override fun get(): Nothing {
            when (error) {
                is ThrowableProvider<Throwable> -> {
                    throw error.throwable()
                }
                is Throwable -> {
                    throw error
                }
                else -> {
                    throw NonThrowableFailureUnwrappingException(this)
                }
            }
        }

        override fun toString(): String {
            return "${Failure::class.simpleName}($error)"
        }
    }
    
    abstract fun get(): T

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure

}

```
