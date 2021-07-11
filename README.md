# 1. Idiomatic Error Handling as a first class domain citizen

- [1. Idiomatic Error Handling as a first class domain citizen](#1-idiomatic-error-handling-as-a-first-class-domain-citizen)
  - [1.1. Introducing `resultk.Result`](#11-introducing-resultkresult)
    - [1.1.1. Simple Usage](#111-simple-usage)
      - [1.1.1.1. Declaring an error codes for your domain:](#1111-declaring-an-error-codes-for-your-domain)
      - [1.1.1.2. Returning a `Result` instance](#1112-returning-a-result-instance)
      - [1.1.1.3. Using a `Result` from an call](#1113-using-a-result-from-an-call)
        - [1.1.1.3.1. Just getting the success value](#11131-just-getting-the-success-value)
        - [1.1.1.3.2. First checking for an error code](#11132-first-checking-for-an-error-code)
        - [1.1.1.3.3. Using the `try-catch`](#11133-using-the-try-catch)
    - [1.1.2. Functional Usage](#112-functional-usage)
      - [1.1.2.1. Mapping from one error to another](#1121-mapping-from-one-error-to-another)
      - [1.1.2.2. Mapping from one value to another](#1122-mapping-from-one-value-to-another)
      - [1.1.2.3. Mapping result to a single value](#1123-mapping-result-to-a-single-value)
      - [1.1.2.4. Inspecting success values and failure](#1124-inspecting-success-values-and-failure)
      - [1.1.2.5. Conditionally handling errors and success values.](#1125-conditionally-handling-errors-and-success-values)
  - [1.2. Advance Modelling of error Codes](#12-advance-modelling-of-error-codes)

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

As an example of error as first class domain citizen, consider the some error codes reported by the Stripe[^1] API:

- `billing_invalid_mandate`
- `card_declined`

There are many more, but having these as first class implementation could look something like this:

```kotlin
fun registerCardPaymentMethod(card: Card): Result<ProcessorFailure,PaymentMethod> {
    //
}
```

## 1.1. Introducing `resultk.Result`

With this out of the way, lets introduce the `Result` type:

```kotlin
sealed class Result<out E, out T> {

    data class Failure(val error: E): Result<E, Nothing>
    data class Success(val value: T): Result<Nothing, E>

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure

    abstract get(): T
}
```

Result is a sealed type which represents a result of an operation: A result can either be a Success, or an Failure. This type provides precise functional control over errors, include the standard Kotlin Throwable class.

This implementation achieves precise control even over the use of exceptions as errors by handling any call in wrapped in a special `result { ... }` code block in a very specific manner: 

- If the caller specifies an exception as error via the `Result<E,*>` the exception is captured as per normal contract, otherwise it is thrown.
- If the caller calls `Result.get `and the captured `Failure.error `is an actual `kotlin.Throwable`, it will be thrown, (again as is the normal Object-Oriented way).
- If the caller calls `Result.get` and the captured `Failure.error`, is not something which can be thrown, this library will wrap it as `WrappedFailureAsException`, and throw it.

Furthermore, this library provides a rich set of functions to transform/process either the `Result.Success.value`, or in the case the underlying `Result.Failure.error` value. These operations are roughly grouped as follows:

- Operations which maps from one type of E, or T to another via the family of mapping operators.
- Operations which retrieves the expected success value (T) via a family of get operations
- Operations which retrieve the possible error value (E) via a family of get operations.
- Operations which take/not take an error, or value, based on a supplied predicate.
- Operations which transform either the success value, or the error value to another type.
- Terminal operations which will only be triggered in either the presence of an error or success value.

Lastly, just a note on interop with the the standard `kotlin.Result`. The library provides the following convenience operations to transform a Result to the standard `kotlin.Result `type (and visa versa):
S
- `resultk.interop.toResult` to convert a `kotlin.Result` to this implementation.
- `resultk.interop.toStandard` to convert this result implementation to the standard `kotlin.Result` type.

### 1.1.1. Simple Usage

#### 1.1.1.1. Declaring an error codes for your domain:

```kotlin
enum class ProcessorError {
    InsufficientFunds,
    PaymentDeclined,
    BlackedListenerPermanently,
    UpstreamError,
    UknownAccount
}
```

#### 1.1.1.2. Returning a `Result` instance

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

#### 1.1.1.3. Using a `Result` from an call

At this point lets say you get the balance from the `getBalance` function. There are several ways to work with the result:

##### 1.1.1.3.1. Just getting the success value

```kotlin
val balance = getBalance(account).get()
```

##### 1.1.1.3.2. First checking for an error code

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

##### 1.1.1.3.3. Using the `try-catch`

```kotlin
val balance = getBalance(account)
try {
    println("Your balance is ${balance.get()}")
} catch(e: Excetion) {
    val err = e.unwrapFailure<ProcessorError>().error
    handleErrorCode(err)
}
```

### 1.1.2. Functional Usage

The `Result` type also exposes several useful functional approaches to error handling. Here are the most common ones:

#### 1.1.2.1. Mapping from one error to another

```kotlin
val : Result<ServiceErrorCode,Long> =
        result<ServiceException,Long> {
            readCounts()
        }.mapFailure { ex ->
            ex.code
        }
```

#### 1.1.2.2. Mapping from one value to another

Given a function which check if a file exists:

```kotlin
val fileCheck = File("test.data").check()
```

Now convert to file size:

```kotlin
val fileSize = fileCheck.map(File::length)
```

#### 1.1.2.3. Mapping result to a single value

```kotlin
// Using kotlin built in let function
val fileSizeInBytes: Long = fileSize.let { (r,e) -> if (e == null) -1 else r.get()  }
// Using funtional version of get:
val fileSizeInByres: Long = fileSize.getOr { -1 }
```

#### 1.1.2.4. Inspecting success values and failure

The library also provide various functions to inspect both success values and failure errors:

```kotlin
// Check if resutl is success or failure:
println("fileSize is success : ${fileSize.isSuccess}")
println("fileSize is failure : ${fileSize.isFailure}")

// Getting error value
val errorCode: ErrorCode? = fileSize.getErrorOrNull()
val errorCodeOption: Optional<ErrorCode> = fileSize.getErrorOptional()
```

#### 1.1.2.5. Conditionally handling errors and success values.

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

## 1.2. Advance Modelling of error Codes

The `resultk.Result` removes most of the burden to use exceptions as error modelling on a domain. This opens up the door for a more expressive modeling.

Bellow is an example which:

- Provides error codes which are both constant (like enums), and normal classes.
- Retrieves detail error messages based on language resource bundles.
- Caters for known and unknown error codes, including upstream provided ones.
- Includes detailed description to upstream error codes.
- Throws a custom error if the case where caller does not handle the error

```kotlin
// file: resultk/demo/acmepayments/PaymentProcessorError.kt
package resultk.demo.acmepayments

import resultk.Result
import resultk.onSuccess
import resultk.result
import resultk.success
import java.util.*
import java.util.ResourceBundle.getBundle

sealed class PaymentProcessorError(val code: String) : Result.Failure.ThrowableProvider {

    protected val messageKey = "error.$code"

    object PaymentDeclined : PaymentProcessorError("payment_declined")
    object BlackedListenerPermanently : PaymentProcessorError("blacklisted_permanently")
    object InsufficientFunds : PaymentProcessorError("insufficient_funds")

    open fun message(): String = message(messageKey).get()
    override fun throwable(): Throwable = PaymentProcessorException(this)

    class UpstreamError(
        val upstreamProvider: String,
        val upstreamErrorCode: String,
        val upstreamProviderErrorMessage: String?
    ) : PaymentProcessorError("upstream.$upstreamProvider.$upstreamErrorCode") {

       
        private fun getDetailsMessageKey() = "$messageKey.details"

        override fun message(): String {
            val generalMessage = message("error.upstream").get()
            val knownUpstreamMessage = message(getDetailsMessageKey())
            return buildString {
                fun appendSentence(sentence: String) {
                    if (isNotEmpty()) {
                        if (last() != '.') append('.')
                        append(' ')
                    }
                    append(sentence)
                }
                append(generalMessage)
                knownUpstreamMessage.onSuccess(::appendSentence)
                upstreamProviderErrorMessage?.also(::appendSentence)
            }
        }
    }

    protected fun message(key: String, vararg args: Any?)
    	: Result<MissingResourceException, String> = result {
            val message = getBundle(PAYMENT_PROCESSOR_MESSAGES).getString(key)
            if (args.isEmpty()) message.success() else message.format(* args).success()
        }
    }

    companion object {
        const val PAYMENT_PROCESSOR_MESSAGES = "/resultk/demo/acmepayments/PaymentProcessorMessages"
    }
}

```

And here is the custom error:

```kotlin
// file: resultk/demo/acmepayments/PaymentProcessorException.kt
package resultk.demo.acmepayments

class PaymentProcessorException internal constructor(
    val error: PaymentProcessorError,
    message: String = error.message(),
) : RuntimeException(message)
```

And this is the English resource bundle: 

```properties
# file: resultk/demo/acmepayments/PaymentProcessorMessages_en.properties
error.payment_declined=Sorry, your payment has been declined. Please contact Acme Payments for more details.
error.blacklisted_permanently=Please contact Acme Payments urgently in regards to this matter.
error.insufficient_funds=Unable to complete this transaction: There is no sufficient funds available.
error.upstreamn=Please try again later.
```
---

[^1]: [Stripe Error codes](https://stripe.com/docs/error-codes)

