# ResultK Core Library

## Short Introduction

The central abstraction of this library is a `Result` hierarchy which is roughly based on the Either monad.

But there are a few important differences: Unlike the either monad, (which is a general structure), the `Result` type is a specialized structure dealing with the expression of (domain) specific errors. But, similar to the either monad, it also assumes that such domain errors should always occur in response, and within the context, of a request by a caller.

For example, consider this snippet:

```kotlin
val (nextPayDate,payDayError) = computeNexPayDate(empGrade, empId)
```

In this sense this result behaves exactly like an Either monad:  Either the caller will be able to get the `nextPayDate`, or there is a `payDayError` while calculated the next pay date.

From here onwards the behavior is very specific in the following manner:

1. `nextPayDate` is just a holder which assumes that the caller would have dealt with the pay day error. Not retrieving the value will have no side effect other than allocation of the variable.
2. If the caller fails to deal with domain error in question, calling the `nextPayDate.value` will turn this call into an application failure by raising an runtime exception.

These important distinctions ensure that:

- Domain errors are always handled, and not handling them will be treated as an application (e.g. `Exception`) failure.
- Control is always in the hands of the caller, even in the case of an domain error.
- Having this `Result<E,T>` type maintains a clear distinction between domain errors, and application failures: Rather have a proliferation of domain errors than exceptions which has the potential of taking control away from the caller.

Thus the whole purpose is to keep on the _**happy path**_!

## The happy path

The *happy path* refers to a specific logical flow, typically defined by some use case.

Sometimes a application fail due to some edge case not being handled in the implementation, or something occurring outside of the control of the application: For example, a missing file, or network connection failure. In this case any logic coded is not strictly part of thus happy path.

In the Object Orientated world, these situation are expressed as an `Exception` which are dealt by catching such exceptions. Having such exception handling code may obfuscate the original use-case to the point of turning a simple, clear cut implementation into a mine field of unintended consequences. This becomes even more problematic when the developer blurs the line between domain specific errors (e.g intended or per designed error conditions), and application/infrastructure failures.

One the main objectives of this library to assist the developer in keeping on this happy path by shrink-wrapping certain patterns and conventions and exposing these abstractions via a friendly DSL like syntax.

Furthermore this library recognized that most developers choose to walk this happy path by either employing a functional style or imperative style. And caters for both styles where it makes sense.

## How to use it

### A function returning a result

A function returning will result in a single instance of typw `Result<E,T>`. By convention the left type (E) represents the specific error. From here onwards there are many ways you can go (staying with `computeNextPayDate()` function.

The _ResultK_ offers 3 standard ways of to deal with such functions:

1. Decomposing the result into a possible success value, and an application error. In such a case the application error maybe `null` . In such a case a developer must test for a null error before continuing on the happy path:

    ```kotlin
    val (nextPayDate,payDayError) = computeNexPayDate(empGrade, empId)
    if (payDayError != null) {
        // Handle error
        return ...
    }
    println(" Your pay next pay day is: ${nextPayDate.value}")
    ```

2. A developer my also choose to ignore the error and treat the presence of the error as `null` value:

   ```kotlin
    val nextPayDate = computeNexPayDate(empGrade, empId)
    println(" Your pay next pay day is: ${nextPayDate.orNull()}")
   ```

3. Transform the error into your own specific error, or message:

   ```kotlin
    val (nextPayDate,errMessage) = computeNexPayDate(empGrade, empId)
        .mapError { it.message() } // Asking for message!
   
    if (errMessage != null) {
        System.err.println("Unable to calculate next pay day : $errMessage")
        return
    }
    println("Your next pay day is: ${nextPayDate.value}")
   ```

The are more ways to access results, here just a list of some of the functions this library provides.

| Intention                                                       | Example                        |
| --------------------------------------------------------------- | ------------------------------ |
| Get a value, or convert the error to a value (of the same type) | `or((E) -> T): T`              |
| Get a value, or supply a default value in the case of an error  | `or(default): T`               |
| Get a value, or `null`                                          | `orNull(): T?`                 |
| Get a value, or throw an exception based on the domain error    | `orThrow((E) -> Exception): T` |
| Retrieve a domain error, or `null`                              | `errorOrNull(): E?`            |

### Constructing results

You could construct a `Result` directly, but the best way is to use the `resultOf<E,T>{}` function, for example:

```kotlin
fun computeNextPayDate(
    empGrade: Grade, 
    empId: String
): Result<LocalDate,PaymentError> {
    return resultOf<PaymentError,LocalDate> {
        if (empGrade != GradeActive) {
            PaymentError.InactiveEmployee(empId).failure()
        } if (empId !in employeeRepo) else {
            PaymentError.UnknownEmployee(empId).failure()
        } else {
            val paymentDate = LocalDate.now().nextPayDate()
            paymentDate.success()
        }
    }
}
```

This has several benefits:

1. Everything within the `resultOf<E,T> { .. }` block will be wrapped in a `try-catch` which catches raised domain errors.
2. If the exception is of type `<E>`, it will return as normal domain error, and not treat the exception as an application failure.
3. If the exception act as wrapper, and contains a `Failure<E>`, it will simply unwrap the failure, and return it.
4. The function actually knows the exact type of `<E>`, and will treat all other kind errors (not handled) as application failures.

### Walking the happy path

Up until now, all the examples demonstrated the imperative style of keeping on the happy path. A functional/hybrid style offers a more fluent style of keeping on, (and returning to), the happy path.

To demonstrate this, consider the following function which will create/ensure that a required `java.io.File` exists:

```kotlin
fun File.required(
    kind: FileKind.Known,
    includeParents: Boolean = true,
    createIfNotExists: Boolean = true
): Result<FileRequirementError, File> {

    if (exists()) {
        return when (val actualKind = kind().or(FileKind.Unknown)) {
            kind -> success()
            else -> FileRequirementError.ExistsAlreadyAs(actualKind).failure()
        }
    }

    if (!parentFile.exists() && !includeParents) {
        return FileRequirementError.ParentPathDoesNotExists(path).failure()
    }

    val fileSystemError = fun(ex: IOException): IOUnexpectedIOFailure {
        return IOUnexpectedIOFailure(
            kind,
            path,
            ex
        )
    }

    return resultWithHandlingOf(fileSystemError) {
        if (!createIfNotExists) {
            throw FileNotFoundException(
                "Required ${kind.name} does not exists: $path"
            )
        }
        when (kind) {
            FileKind.Directory -> if (!mkdirs()) {
                throw IOException("unable to create directory: $path")
            }
            FileKind.RegularFile -> if (!createNewFile()) {
                throw IOException("unable to create regular file: $path")
            }
        }
        success()
    }
}
```

Note the following:

- We declare up front a function to handle low level I/O exceptions.
- We are passing in a function which produces an appropriate domain error.
- We are also passing in a function which is responsible to create the file or directory if does not exists.
- Throwing an `IOException` will never cause us to loos control.
- We are also not capturing unnecessary application/system level exceptions.
- We only focus on our domain, which by design only care about the following errors:

```kotlin
sealed class FileRequirementError {
    data class ExistsAlreadyAs(val kind: FileKind) : FileRequirementError()
    data class ParentPathDoesNotExists(val path: String) : FileRequirementError()
    data class UnexpectedIOFailure(
        val kind: FileKind,
        val path: String,
        val cause: IOException
    ) : FileRequirementError(), 
            ThrowableProvider<IOException> by ThrowableProvider.of(cause)
}
```

## Exploring further

The library offers a rich set of functional operators to those which prefer a fully functional style of expression, as well as hooks into the wrapping and/or unwrapping of domain errors in the case of the developer not handling said domain error. The hooks can be exploited to handle cross-cutting concerns.

### Functional Support

#### Mapping errors and values from one type to another

| Intention                                                          | Operation                            |
| ------------------------------------------------------------------ | ------------------------------------ |
| Mapping a success value to either another type of success or error | `r.map((T) -> R):Result<E, R>`       |
| Mapping a error value to either another type of success or error   | `r.mapError((E) -> R): Result<R, T>` |
| Folding either a success value, or error into a single value       | `r.fold((E) -> R,(T) -> R):R`        |

#### Flow Control

| Intention                                                                  | Operation                                                                    |
| -------------------------------------------------------------------------- | ---------------------------------------------------------------------------- |
| Flow into a next logical step while keep on handling errors                | `thenResultOf((T) -> Result<E, R>): Result<E, R>`                            |
| Flow into a next logical step and decide up front how to handle exceptions | `thenResultWithHandling((Exception) -> E,(T) -> Result<E, R>): Result<E, R>` |
| Give up flow control, and raise an error as application failure            | `raise(E)`                                                                   |

Here is an (bit of contrived) example of using these operations:

```kotlin

fun getRecentArticlesByUser(userId: String, limit: Int)
    : Error<ApiError,Map<LocalDate,List<ArticleAction>>> {

    val (user, findUserErr) = userRepo.findByIdOrNull(userId)

    if (findUserErr != null) {
        return ApiError.InternalError(findUserErr)
    }

    val interactions = setOf(
        Interaction.SeenBy(user.value),
        Interaction.VotedUpBy(user.value),
        Interaction.NotVotedDownByUser(user.value)
        Interaction.RatedBy(user.value)
        Interaction.FirstCommentedOnBy(user.value),
        Interaction.RepliedToCommentBy(user.value)
    )

    val eventToAction = fun(event: ArticleEvent) = ArticleAction(
        type = event.type.name(),
        title = event.article.shortTitle
    )

    return articles.findRecentEventsByInteraction(interactions,limit)
        .thenResultOf { articles -> articles.groupBy { a -> a.date.toLocalDate() }.success() }
        .thenResultOf { map -> map.mapValues(eventToAction).success()  }
        .mapError { e -> ApiError.InternalError(e) }

}

```

### Integration & cross cutting concerns

This library also provide hooks which can be used to handle/re-defined certain cross cutting concerns such as:

1. Redefine how a domain error type is expressed as an application failure.
2. The manner in which error codes can unwrapped from exceptions.

These mechanisms are used internally, and any developer may hook into the underlying implementation by implementing the correct interface(s) where appropriate.

As a concrete example, consider the following class which wraps/unwraps domain errors from exceptions:

```kotlin
class DefaultFailureUnwrappingException(
    wrapped: Failure<*>,
) : RuntimeException("${wrapped.error}"), FailureUnwrappingCapable<Any> {
    private val _wrapped = wrapped as Failure<Any>
    override fun unwrapFailure(): Failure<out Any> = _wrapped
}
```

Any error implementing the following interface controls which exceptions are actually thrown by the `raise(E)` function:

```kotlin
fun interface ThrowableProvider<out X : Throwable> {
    fun throwing(): X
}
```
