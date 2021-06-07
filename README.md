# `Result` Implementation for Kotlin 1.5

The `Result` class can be seen as specialized version of the `Either` monad. This monad by conventions allows the caller of function to return either the actual result, or the error. By convention this monad is declared (in general) terms:

```kotlin
abstract class Either<out Left,out Right>
```

By conventions the left side (denoted by `Left`) indicates the error value, followed by the right side for the result. Only one value can be present. Hence the 'either' moniker.

## Design considerations of the Library

This project is an experiment to implement a more fluent and natural implementation of the `Result` by:

- Providing a procedural (non Object Orientated) style of handling exceptions(and errors) in Kotlin.
- Provide a more rigorous handling of errors from a functional perspective.
- At the same time not forcing developers which is more comfortable with the try-catch style of handling exceptions to adopt a new functional style.

## High Level Overview

The Result Library can summarized by the following UML diagram:

![uml](resultk.png)
---

> **Important things to note**:
>
> 1. `ResultOperations` exposes set of  functional style of operations operates on the `Result` hierarchy of classes & objects.
> 2. The`WrappedUnThrowableFailureException` class, and the `WrappedFailureOperations` functions bridge the procedural style of error handling and the more traditional Object Orientated style of `try-catch` prevalent in most OO languages. The former wraps a non throwable error as an exception which can be thrown and caught, while the latter exposes general functions to retrieve wrapped errors in type safe manner.
> 3. The `Success` and `Failure` classes are special containers which stores either an result of successful computation, or the error of failed computation respectively. They are subclasses of the sealed `Result` class hierarchy.

