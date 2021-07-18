# Returning a Result

Remember a Result type may contain either error value (`<E>`), or a success (`<T>`) value. The easiest way to return either of these is to use the one of t extensions functions provided:

- `<T>.success<T>()` for a success value, and
- `<E>.failure<E>()` for a failure value.

These functions can be used anywhere, but they are really intended to be used within a `result {}` code block, for example:

```kotlin
```

Things to note:
