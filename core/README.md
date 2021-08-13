# ResultK Core Library

## Short Introduction

The central abstraction of this library is a `Result` hierarchy which is roughly based on the Either monad, but with a few important differences: Unlike the either monad, which is a general structure, the `Result` type is a specialized structure dealing with the general expression of an (domain) error. But, similar to the either monad, it also assumes that such an error should always occur in response to an expected output.

For example:

```kotlin
val (nextPayDate,payDayError) = computeNexPayDate(empGrade, empId)
```

In this sense this result is exactly like a either. Given the example above: Either the caller will be able to get the `nextPayDate` or there have been a `payDayError` while calculating the next pay date.

Up until not the result behaves exactly like the Either monad. But from here onwards the behavior is very specific in the following manner:

1. `nextPayDate` is just a holder which assumes that the caller would have dealt with the pay day error.
2. If this is not the case then using the `nextPayDate.value` will turn this into an application failure by raising an runtime exception.

This is intentionally, and keeping with the spirit that a developer should have acted on the application error, and is thus an application failure.

Also note that unlike the general application failure, this logic will only  be triggered in an attempt to access the underlying value.

## How to use it

### A function returning a result

A function returning a result will return a single instance of `Result<E,T>`, and by convention the left type (E) represents the specific error. From here onwards there are many ways you can go (staying with `computeNextPayDate()` function.

1. Decomposing the result into a possible success value, and an application error. In such a case the application error will actually be `null` which is can be used to check for the result before continuing on the happy path.
2. Ignore the error and treat the presence of the error as `null` value.
3. Transform the error into your own specific domain error (if it from another domain).

### Constructing results

### Walking on the happy path
