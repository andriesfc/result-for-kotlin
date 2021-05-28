# Notes

## Problems with Kotlin's `Result<T>` implementation

Let's look at a typical use of Kotlin's standard library type `Result`:

```kotlin
val text = file.runCatching {  readText() }
```

1. Catch the world.
2. Consequently, forces caller to deal with the world.
3. Can eat up exceptions by hiding behind the `getOrNull()` function (this includes low level exceptions).

