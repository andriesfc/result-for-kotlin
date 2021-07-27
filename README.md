# Error Handling in Kotlin as a first class domain concern

Why is error handling an issue in JVM land? I mean, is it actually an issue? This library was born out of some observations and frustrations  while developing enterprise JVM based applications. So while looking for something which is more than just a set of conventions, I decided codify what I believe to be good practices into a library which purposefully steers a developer towards good practices, and at the same time also away from the business as usual model.

So what is wrong with the way we handle domain errors and/or exceptions in JVM land?

I believe the reasons why error handling is not first class domain concern, especially in JMV land, are it is core a misunderstanding of the differences between exceptions as featured in the language, vs domain errors. 

To summarize:

| Exceptions                                                   | Domain Error/Codes                                 |
| ------------------------------------------------------------ | -------------------------------------------------- |
| Indicates that an app could not handle a system error.       | Indicates the caller should handle the error.      |
| Raising an exception exits the happy path.                   | Returning a domain error/code has no side effect.  |
| Catching an exception can be very expensive.                 | An error code is just another variable.            |
| Exceptions are designed to be caught as an application failure. | Domain errors are designed to advise control flow. |
| Exceptions models the runtime/host failure domain.           | Domain errors models the business domain.          |

As a consequence consider that:

1. Creating an exceptions may not be that expensive, but catching it is expensive. Usually this involves unrolling the call stack.
2. Throwing an exception also means that the code throwing the exception looses all flow control. Nothing wrong with this, as this is the actually intended use of exceptions.
3. Catching exceptions leads to subtle errors which can sometime be hard to pin down due to the following reasons:
   - More often than not, such caught exceptions are far removed from the offending code/cause. Consequently, a developer has to spend much effort to track and understand the error handing code which  some times live deep in the bowls of may nested levels deep if `try-catch` statements.
   - Sometimes an application would catch an exception which should never be handled under a `try-catch-all` statement, for example an `OutOfMemoryException`. If a developer forgets to log the error, the actual cause can sometimes just disappear leading to many man-hours hunting for something which should have been easy to fix: For example, give the process more memory, or finding the data structure leaking the memory.
4. The very act of raising an exception also has some serious untended consequences insofar as the domain driven design/modelling:
   - Causes the boundaries of one domain to flow into with another,
   - It is almost impossible to design for this, as each exception thrown is like bullet punching holes in any well crafted domain boundary.
   - Each time such a "domain exception" is thrown (thus the lost control of flow in the domain throwing it), will almost certainly result in all other domains models to be invalidated.
   - Ultimately this would mean that each domain has to have deep knowledge of almost all errors on every other domain in the application.
5. Lastly, Java's unfortunate decision to have checked exceptions just compounds the problems by encouraging developers to wrap these checked exceptions into runtime exceptions. Most the time these wrapped exceptions has very little bearing on the domain and exists only because of the compiler forcing the developer to do so. On a practical level this has the consequence of hiding the underlying errors deep into logs and many-line stack traces deep.

> ❗️ Exceptions are not undesirable, as long as they are used as intended.

## Criteria for Domain Error modelling as a first class concern

1. Exceptions should model application failures, not domain errors.
2. Domain errors should model your business domain, not your application runtime/infrastructure.
3. The handling of Domain Errors should not be to fare removed from the code which produces such a domain error:
  
   - Remember error codes are control flow advice.
   - Ask yourself: If handling of such error code is to far removed, are you still acting appropriate on the advice?
4. When it comes to exceptions which are not your own:

   - Decide upfront how you are going handle them.
   - Be very careful when using a `try-catch-all` exception handler, if you need one, it is best to always throw what you cannot handle.
   - Only handle exceptions which are appropriate to your domain you're implementing.
   - Have only 3 kind of exception handlers:
      1. To log and throw.
      2. To map a domain appropriate thrown exception to a domain specific error code.
      3. To not have one is sometimes a better choice 😈.
5. Make sure domain errors produces messages  which makes sense to the consumer of your domain.
6. Make sure domain errors also produces messages which are enriched for developers and devops personal.
8. Make sure such messages can be produced in locale specific manner. 
9. IMPORTANT ⚠️: Make sure that you can actually raise a domain error as an exception. This will be clear indicator that your implementation is not complete!
9. Implement a test harness for each of domain error type which asserts at the very least: 
   - That localized message are produced for each the type of audioance.
   - That any exceptions you choose to propagate as a cause will not get lost when your domain error is thrown/consumes.



## Introducing `Resultk`

This smallish project aims to bring these concerns to together into a single library in Kotlin. I choose Kotlin (and not Java) for the following reasons: 

- It has the notion of a sealed types (in Scala this called 'case' types). Using a sealed type means the developer can rely on the compiler to pick up on things such unhandled error codes.
- Kotlin nullable types are useful, and unlike Java not source of errors to be picked up at runtime.
- The Kotlin compiler offers reified types. This means the library can distinguish between the one error type vs another type, even when using generics.
- Kotlin offers extensions (which are just static functions under the hood), which means the library is able to model domain error handling around even types from 3rd party libraries.

## Show me how to it used

Let's say you have an extension function which takes an algorithm name of message digester algorithm, and produce a hexadecimal hash of the whole stream. The function & error cases follows as such:

```kotlin
// The hash function
fun InputStream.hash(algorithm: String): Result<DigesterError,String> {
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
5. 💀 BAD - Calling `get()` and failing to handle all values of `err` will result in an exception to be thrown. This obviously not good and could lead to the unexpected death of the application.

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

