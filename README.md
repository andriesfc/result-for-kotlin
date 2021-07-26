# Error Handling in Kotlin as a first class domain concern

I believe the standard practice of using exceptions as business/domain error handing is detrimental to the enterprise for the following reasons:

1. Creating exceptions are expensive, as at is involves unrolling the call stack. For example sometimes embedded C++ systems would due to severe device constraints disable to compiler from generating code to unroll the stack.
2. Throwing an exception also means the code throwing the exception looses all flow control. Nothing wrong with this, as this is actually the intended use of exceptions.
3. Catching these exceptions is more often than not far removed from the offending code/cause which can mean considerable effort to understand and sometimes locate the real cause.
4. The whole decision to model exceptions as error codes  has some serious consequences: 
   - Allows errors and failures which are domain specific to not only leak into another domain of the application, but in actual fact it is almost impossible to prevent.
   - The act of raising an exception is also the act of loosing control of flow (as designed). Unfortunately this is also true of the caller if the caller fails to catch the appropriate exception.
   - It also forces all domain to know of all other domains due to domain knowledge encoded un such errors.
5. Java's unfortunate decision to have checked exceptions just compounds the problems by encouraging developers to wrap these checked exceptions into runtime exceptions. These wrapped exceptions has very little bearing on the domain, and hides the underlying error deep into logs and many line stack traces.

## Introducing `resulkt`

This smallish library aims to bring error handling as first class domain concern to the applications written Kotlin by:

1. Providing minimal scaffolding to move away from using exceptions to:
   - Model domain/business errors with
   - Use them as control flow mechanisms.
2. Provide functional style control flow mechanisms to deal with error both error flow and success flow.
3. Provide the basis for building rich domain specific errors.
4. Provide functional expressions to encourage developers to think upfront about alternate/error flows when encoding business requirements.
5. Encourage authors to handle errors sooner rather than later.

> **As a side note**: Non-object-oriented languages such as C and Go inspired me to implement this library.

## Show me how to it used

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

// Actual error cases. Only two are supported:

data class DigesterAlgorithmUnknown(val algorithm: String, val cause: NoSuchAlgorithmException)
	: DigestError("error.digister.noSuchAlgorithm", algorithm, cause.message)

data class DigesterFailedToIngestSource(val ioError: IOException)
   : DigestError("error.digister.failedToIngestSource", ioError.message)

```

Calling this function in very imperative manner could look like this:

```kotlin
val (digest, err) = fileStream.hash("sha1")

when(err) {
   is DigesterAlgorithmUnknown -> { 
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

So here is an implementation exploiting the Kotlin compiler to fail when not when the author does not handle all possible error cases:

```kotlin
fileStream.hash("sha1")
   .onFailure { err ->
      when (err) {
         is DigesterAlgorithmUnknown -> { 
            println(err.message())
            err.cause.printStackTrace()
         }
        is DigesterFailedToIngestSource -> {
            println(ioError.message())
            err.ioError.printStackTrace()
         } 
         // Notice no `else ->` 
      }
   }.onSuccess { digest ->
      println(digest)
   }
```

This is just a small introduction how rich and clean error handling can be.

