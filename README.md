# Error Handling in Kotlin as a first class domain concern

I believe the standard practice of using exceptions as business/domain error handing is detrimental to the enterprise for the following reasons:

1. Creating exceptions are expensive, as at is involves unrolling the call stack. For example sometimes embedded C++ systems would due to severe device constraints disable to compiler from generating code to unroll the stack.
2. Throwing an exception also means the code throwing the exception looses all flow control. Nothing wrong with this, as this is actually the intended use of exceptions.
3. Catching these exceptions is more often than not far removed from the offending code/cause which can mean considerable effort to understand and sometimes locate the real cause.
4. The whole decision to model exceptions as error codes  has some serious consequences: 
   - Allows errors and failures which are domain specific to not only leak into another domain of the application, but in actual fact it is almost impossible to prevent.
   - The act of raising an exception is also the act of loosing control of flow (as designed). Unfortunately this is also true of the caller if the caller fails to catch the appropriate exception.
   - It also forces all domain to know of all other domains due to domain knowledge encoded un such errors.
5. Java's unfortunate decision to have checked exceptions just compounds the problems by encouraging developers to wrap these checked exceptions into runtime exceptions which often has no direct bearing on the problem the domain it tries to solve.

## Introducing `resulkt`

This smallish library aims to bring error handling as first class domain concern to the applications written Kotlin. By first class concern I mean:

1. A conscience effort to stop using exceptions to:
   - Model domain/business errors 
   - Use them as control flow mechanisms. 
2. Provide minimal control flow mechanisms to deal with error flow.
3. Provide the basis for building rich domain specific errors.
4. Provide functional expressions to encourage developers to think upfront about alternate/error flows when encoding business requirements.
5. Encourage authors to handle errors sooner rather than later.
6. Encourage authors to handle errors 1st and not just as it happens 20 somewhat lines down within `catch` statement.

> **As a side note**: Non-object-oriented languages such as C and Go inspired me to implement this library.

## Show me how to use it

Let's say you have an extension function which takes an algorithm name of message digester algorithm, and produce a hexadecimal hash of the whole stream. The function & error cases follows as such:

```kotlin
// The hash function
fun InputStream.hash(algorithm: String): Result[DigesterError,String] {
   // elided for brevity
}

// A human friendly error where each type contain only what is needed
sealed class DigestError(private val messageKey: String, private vararg messageArgs: Any?) {
    fun message() = ResourceBundle.getBundle("Messages").run {
        when {
            messageArgs.isEmpty() -> getString(messageKey)
            else -> getString(messageKey).format(* messageArgs)
        }
    }
}

// Actual error casses, only two are supported:

data class DigisterAlgorithmUnknown(val algorithm: String, val cause: NoSuchAlgorithmException)
	: DigestError("error.digister.noSuchAlgorithm", algorithm, cause.message)

data class DigesterFailedToIngestSource(val ioError: IOException)
   : DigestError("error.digister.failedToIngestSource", ioError.message)

```

Calling this function in very imperative manner could look like this:

```kotlin
val (digest, err) = fileStream.hash("sha1")

when(err) {
   is DigisterAlgorithmUnknown -> { 
      println(err.message())
      err.cause.printStackTrace()
   }
   is DigesterFailedToIngestSource -> {
      println(ioError.message())
      ioError.cause.printStackTrace()
   }
   else -> {
      println(digest.get())
   }
}

```

Things to note about this implementation: 

1. ✔ GOOD - Line #1 splits into two values:  The first a possible result, and 2nd error value which may be `null`.
2. ✔ GOOD - When the `err` value is null, it is safe to call the `get()` on the result.
3. ✔ GOOD - Error handle takes precedence over result handling.
4. ✔ GOOD - Error messages are for humans as they are build from a locale specific resource file.
5. 💀 BAD - Calling `get()` and failing to handle all values of `err` will result in an exception to be thrown. This obviously not good and could lead to unexpected death of the application.

The last point suggest that there is a better way to handle calling `get()`. 

So here is an implementation exploiting the Kotlin compiler to fail when not all possible error cases are handled within the `when` statement:

```kotlin
fileStream.hash("sha1")
   .onFailure { err ->
      when (err) {
         is DigisterAlgorithmUknown -> { 
            println(err.message())
            err.cause.printStackTrace()
         }
        is DigesterFailedToInngestSource -> {
            println(ioError.message())
            err.ioError.printStackTrace()
         } 
         // Notice no `else ->` 
      }
   }.onSuccess { digest ->
      println(digest)
   }
```

This is just a small introduction how rich an clean error handling can be.

So what else is in this library?

## Library Overview

The library itself can be divided into:

- Core data types
- Various ways to create `Result` instances
- Functions to access success value and errors in fluent manner
- Various ways to map success and failure errors
- A few functions to enable the processing of results in fluent functional manner.
- A private error wrapper to make sure that any un handled error will always result in the very least in a plain old `kotlin.Throwable` instance. 
- Extensions to assist in interop with Kotlin's own `kotlin.Result<T>` class.
- Extensions to deal with `java.util.Optional` type
 
## In addition the following are also included

- Unit tests which test which deal with: 
  - Main library concerns
  - Interop with Java's optional type.
  - Interop with Kotlin type.
  - Various fully fledged out demos which various from simple, to complex: 
    - Demonstrate how usage of the library progress from the old to _try-catch-style_ to fully functional implementation.
    - An example of advance error modeling which caters known and unknown errors, including how to fully control how the library handles exceptions and un handled errors.
- 