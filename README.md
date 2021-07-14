# Idiomatic Error Handling as a first class domain citizen

_ResultK_ is a smallish library to bring error handling as first class concern to a domain implementation. The traditional way for any Object Orientated language is to resort to either special Enums, or constants, or what is more common nowadays, throwing exceptions.

Consider the unintended consequences of using exceptions as error handling within a domain:

- Most of the time, the business code throwing the exception is far removed from where the caught exception is handled. This can range from many lines, to even files which are not even in the same project.
- Throwing and catching an exception is an expensive operation, as the JVM brings with it stack unwrapping as well as the associated complex data structures to support it during runtime.
- Real business errors gets lost in translation because of the practice of wrapping one exception into another. This is even more pronounce in Java which employs checked exceptions.

I’m sure this list can be expanded on, but these are the high level reasons which motivated the creation of this library.

The traditional `Either` monad is very common in functional languages such as Haskel & Scala. By convention the left side indicates that a computation has not completed with an expected result, but rather an error. Conversely, the right side is refers to the expected result of an operation. It is also important to keep in mind that this is just a convention, and some uses `Either` may use the left and right side for different purposes.

Because of this, this library implements a special variant of the classic `Either<Left,Right>` monad, with some important differences:

1. This `Result` type _always_ treats the left side as an **error**.
2. This `Result` type _always_ treats the right side as a **success** value. 
3. This library will always decompose the result value in the pattern of `(result,error?)`

As an example of error as first class domain citizen, consider the some error codes reported by the Stripe API:

- `billing_invalid_mandate`
- `card_declined`

There are many more, but having these as first class implementation could look something like this:

```kotlin
fun registerCardPaymentMethod(card: Card): Result<ProcessorFailure,PaymentMethod> {
    //
}
```

## Introducing `resultk.Result`

With this out of the way, lets introduce the `Result` type:

```kotlin
sealed class Result<out E, out T> {

    data class Failure(val error: E): Result<E, Nothing>
    data class Success(val value: T): Result<Nothing, E>

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure

}
```

Result is a sealed type which represents a result of an operation: A result can either be a `Success`, or an `Failure`. This models the data structure to deal with errors and success values (including the standard `kotlin.Throwable` class).

This implementation achieves precise control even over the use of exceptions as errors by all calls wrapped in a special `result { ... }` code block in a very specific manner: 

- If the caller specifies an exception as error via the `Result<E,*>` the exception is captured as per normal contract, otherwise it is thrown.
- If the caller calls `Result.get `and the captured `Failure.error `is an actual `kotlin.Throwable`, it will be thrown, (again as is the normal Object-Oriented way).
- If the caller calls `Result.get` and the captured `Failure.error`, is not something which can be thrown, this library will wrap it as `WrappedFailureAsException`, and throw it.

Furthermore, this library provides a rich set of functions to transform/process either the `Result.Success.value`, or in the case the underlying `Result.Failure.error` value. These operations are roughly grouped as follows:

- Operations which maps from one type of error, or value to another type via the family of mapping operators.
- Operations which retrieves the expected success value (T) via a family of get operations
- Operations which retrieve the possible error value (E) via a family of get operations.
- Operations which take/not take an error, or value, based on a supplied predicate.
- Operations which transform either the success value, or the error value to another type.
- Terminal operations which will only be triggered in either the presence of an error or success value.

Lastly, just a note on interop with the the standard `kotlin.Result`. The library provides the following convenience operations to transform a Result to the standard `kotlin.Result `type (and visa versa):
- `resultk.interop.toResult` to convert a `kotlin.Result` to this implementation.
- `resultk.interop.toStandard` to convert this result implementation to the standard `kotlin.Result` type.

### Simple Usage

#### Declaring an error codes for your domain:

```kotlin
enum class ProcessorError {
    InsufficientFunds,
    PaymentDeclined,
    BlackedListenerPermanently,
    UpstreamError,
    UknownAccount
}
```

#### Returning a `Result` instance

Using the above error codes, as an example:

```kotlin
fun getBalance(accountNo: AccountNo): Result<ProcessorError,Money> = result {

    val (a, accountError) = accounts.get(accountNo)

  	if (accountError != null) {
        return a
    }

    a.get().balance().success()

}
```

#### Using a `Result` from an call

At this point lets say you get the balance from the `getBalance` function. There are several ways to work with the result:

##### Just getting the success value

```kotlin
val balance = getBalance(account).get()
```

##### First checking for an error code

```kotlin
val (balance,err) = getBalance(account)
if (err != null) {
    // handle error
    handleErrorCode(err)
} else {
    // Do something with the balance:
    println("Your balance is :${balance.get()}")
}
```

##### Using the `try-catch`

```kotlin
val balance = getBalance(account)
try {
    println("Your balance is ${balance.get()}")
} catch(e: Excetion) {
    val err = e.unwrapFailure<ProcessorError>().error
    handleErrorCode(err)
}
```

### Functional Usage

The `Result` type also exposes several useful functional approaches to error handling. Here are the most common ones:

#### Mapping from one error to another

```kotlin
val : Result<ServiceErrorCode,Long> =
        result<ServiceException,Long> {
            readCounts()
        }.mapFailure { ex ->
            ex.code
        }
```

#### Mapping from one value to another

Given a function which check if a file exists:

```kotlin
val fileCheck = File("test.data").check()
```

Now convert to file size:

```kotlin
val fileSize = fileCheck.map(File::length)
```

#### Mapping result to a single value

```kotlin
// Using kotlin built in let function
val fileSizeInBytes: Long = fileSize.let { (r,e) -> if (e == null) -1 else r.get()  }
// Using funtional version of get:
val fileSizeInByres: Long = fileSize.getOr { -1 }
```

#### Inspecting success values and failure

The library also provide various functions to inspect both success values and failure errors:

```kotlin
// Check if resutl is success or failure:
println("fileSize is success : ${fileSize.isSuccess}")
println("fileSize is failure : ${fileSize.isFailure}")

// Getting error value
val errorCode: ErrorCode? = fileSize.getErrorOrNull()
val errorCodeOption: Optional<ErrorCode> = fileSize.getErrorOptional()
```

#### Conditionally handling errors and success values.

Conditionally handling comes in 2 flavors:

- The set of 1st is seen as terminal operations. These operations do not return any values:

```kotlin
fileSize.onSuccess {
    println("File size = $it")
}
fileSize.onFailure { ex ->
    println("Unable to get file size")
    ex.printStrackTrace()
}
```

- The 2nd flavor actually returns values:

```kotlin
// Take the success value based on predicate, and return -1 for any other err
val bytesFound = fileSize.takeSuccessIf { true }?.get() ?: -1
```

There operations also have mirror counter parts:

|             | Success                          | Failure                      |
| ----------- | -------------------------------- | ---------------------------- |
| take        | `r.takeSuccessIf(predictare)`    | `r.takeFailureIf(predicate)` |
| do not take | `r.takeSuccessUnless(predicate)` | `r.takeUnless(predicate)`    |


## Usage Patterns `resultk.Result` promotes

This library is best use in functional manner. Keeping to the functional style means that error and success processing will always be restricted, and managed by the library. Only when something truly unexpected happens will control flow  exit the happy path.

> It also important to note that using functional call style unifies both, the expected output flow, and the error flow in one "happy path".

## Advance Modeling of error Codes.

The `resultk.Result` removes most of the burden to use exceptions as error modelling on a domain. This opens up the door for a more expressive modeling.

Bellow is an example demonstrating how to:

1. Provides error codes which are both constants (like `enums`), and normal classes.
2. Retrieves detail, and ***helpful***, error messages based on language resource bundles.
3. Caters for both known and unknown upstream errors.
4. Includes detailed description of mapped upstream error codes.
5. Throws a custom error if the case where caller does not handle the error.
6. Seals the hierarchy in order that no other 3d party library can just add add more types of their own. 

Here are some sample error messages to illustrate how helpful such error message could be with this approach:

```
 Upstream provider has not completed the request. 
 The following upstream errors details were reported by provider [new_upstream_provider_701]: [error_code: E_342, error_message: Unconfigured down stream requestor] 
 This upstream error code is not mapped. 
 Please add the following key [error.upstream.new_upstream_provider_701.E_342] to this resource bundle: /resultk/demo/acmepayments/PaymentProcessorMessages [Locale: English]
```

```
Upstream provider has not completed the request. 
This account monthly limit has been exceeded. 
The following upstream errors details were reported by provider 
    [moon68inc]: [error_code: E_660-011, 
        error_message: `**Detail**` error message was not supplied 
        by upstream provider!]
```

```stacktrace
resultk.demo.acmepayments.PaymentProcessorException: 
    Unhandled payment processor error has occurred with the following code [payment_declined]. 
        Details: Sorry, your payment has been declined. Please   contact Acme Payments for more details.
 	at resultk.demo.acmepayments.PaymentProcessorError.throwable(PaymentProcessorError.kt:19)
```


```kotlin
package resultk.demo.acmepayments

import resultk.Result
import resultk.getOrNull
import resultk.result
import resultk.success
import java.util.*
import java.util.ResourceBundle.getBundle

sealed class PaymentProcessorError(val code: String) : Result.Failure.ThrowableProvider {

    internal val messageKey = "error.$code"

    object PaymentDeclined : PaymentProcessorError("payment_declined")
    object BlackedListenerPermanently : PaymentProcessorError("blacklisted_permanently")
    object InsufficientFunds : PaymentProcessorError("insufficient_funds")

    open fun message(): String = message(messageKey).get()
    override fun throwable(): Throwable = PaymentProcessorException(this)
    override fun toString(): String = code

    class UpstreamError(
        val upstreamProvider: String,
        val upstreamErrorCode: String,
        upstreamProviderErrorMessage: String? = null
    ) : PaymentProcessorError("upstream.$upstreamProvider.$upstreamErrorCode") {

        private val detailedMessage = buildString {
            val generalMessage = message("error.upstream").get()
            val mappedUpstreamErrorKey = "error.upstream.$upstreamProvider.$upstreamErrorCode"
            val mappedUpstreamMessage = message(mappedUpstreamErrorKey).getOrNull()
            val upstreamDetailMessage = message(
                "error.upstream.see_details",
                upstreamProvider,
                upstreamErrorCode,
                upstreamProviderErrorMessage ?: notSuppliedByUpstreamProvider
            ).get()
            val noteThatUpstreamIsNotMapped = when (mappedUpstreamMessage) {
                null -> message(
                    "error.upstream.note.unmapped",
                    mappedUpstreamErrorKey,
                    PAYMENT_PROCESSOR_MESSAGES,
                    Locale.getDefault().displayLanguage
                ).get()
                else -> null
            }
            appendSentence(generalMessage)
            appendSentence(mappedUpstreamMessage)
            appendSentence(upstreamDetailMessage)
            appendSentence(noteThatUpstreamIsNotMapped)
        }

        override fun message(): String = detailedMessage
    }


    companion object {
        private val notSuppliedByUpstreamProvider =
        	message("sentence_building.not_supplied_by_upstream_provider").get()
        private val punctuation = message("sentence_building.punctuation").get().toSet()
        private val fullStop = message("sentence_building.fullstop").get().first()
        private val oneSpace = message("sentence_building.onespace").get()
        private val Char.isNotPunctuation: Boolean get() = this !in punctuation
        
        val constants = PaymentProcessorError::class.sealedSubclasses
        	.mapNotNull { it.objectInstance }
        
        internal const val PAYMENT_PROCESSOR_MESSAGES 
        	= "resultk/demo/acmepayments/PaymentProcessorMessages"
        
        internal fun message(key: String, vararg args: Any?)
        	: Result<MissingResourceException, String> {
            return result {
                getBundle(PAYMENT_PROCESSOR_MESSAGES).getString(key).run {
                    when {
                        args.isEmpty() -> this
                        else -> format(* args)
                    }
                }.success()
            }
        }

        private fun StringBuilder.appendSentence(sentence: String?) {
            if (sentence.isNullOrEmpty()) return
            if (isNotEmpty()) {
                if (last().isWhitespace()) trimEnd(Char::isWhitespace)
                if (last().isNotPunctuation) append(fullStop)
                append(oneSpace)
            }
            append(sentence.trimEnd(Char::isWhitespace))
            if (last().isNotPunctuation) append(fullStop)
        }

    }
}

```

And here is the custom error:

```kotlin
package resultk.demo.acmepayments

import java.util.ResourceBundle.getBundle

class PaymentProcessorException internal constructor(
    val error: PaymentProcessorError) : RuntimeException
(
  getBundle(PaymentProcessorError.PAYMENT_PROCESSOR_MESSAGES)
    .getString("paymentProcessorException.message")
    .format(error.code, error.message())
)
```

And this is the English resource bundle: 

```properties
# file: resultk/demo/acmepayments/PaymentProcessorMessages_en.properties
error.payment_declined=Sorry, your payment has been declined. Please contact Acme Payments for more details.
error.blacklisted_permanently=Please contact Acme Payments urgently in regards to this matter.
error.insufficient_funds=Unable to complete this transaction: There is not sufficient funds available.
error.upstream=Upstream provider has not completed the request.
error.upstream.see_details=The following upstream errors details were reported by provider [%s]: [error_code: %s, error_message: %s]
error.upstream.note.unmapped=This upstream error code is not mapped. Please add the following key [%s] to this resource bundle: /%s [Locale: %s]
error.upstream.moon68inc.E_660-011=This account monthly limit has been exceeded.
paymentProcessorException.message=Unhandled payment processor error has occurred with the following code [%s]. Details: %s
sentence_building.fullstop=\u002E
sentence_building.onespace=\u0020
sentence_building.punctuation=.;:"?!,`'<>{}[]
sentence_building.not_supplied_by_upstream_provider=**Detail** error message was not supplied by upstream provider!
```



