# Domain Driven Error Modelling in Kotlin

## Overview

Is modelling of domain errors actually worth the effort to spend money and time? Why is error handling an issue in JVM land? I mean, is it actually an issue?

This project was born out of some observations and frustrations  while developing enterprise JVM based applications. So while looking for something which is more than just a set of conventions, I decided to codify what I believe to be good practices into a single project with the following purpose in mind:

1. To produce living repository guidelines and learnings.
2. To distill these guidelines into a set of libraries to aid, and where possible, enforce some of the guidelines.
3. To steer away from the current laissez-faire attitude towards exceptions, exception and application error handling.

So what is wrong with the way we handle domain errors and/or exceptions in JVM land?

Behold the usual suspects:

1. `try-catch-all` eating up exceptions.
2. Nested `try-catch` statements.
3. Dealing with exceptions is most, of not all, of the proximity of the cause. Such an mental mode us overlay complex and  difficult to maintain under the best of times.
4. Meaningless error messages for users.
5. Meaningless error messages in logs.

I believe the reasons for these issues, (especially in JMV land), stem from a core misunderstanding of the differences between exceptions as featured in the language, vs domain errors.

To summarize these differences:

| Exceptions                                                      | Domain Error/Codes                                 |
| --------------------------------------------------------------- | -------------------------------------------------- |
| Indicates that an app could not handle a system error.          | Indicates the caller should handle the error.      |
| Raising an exception exits the happy path.                      | Returning a domain error/code has no side effect.  |
| Catching an exception can be very expensive.                    | An error code is just another variable.            |
| Exceptions are designed to be caught as an application failure. | Domain errors are designed to advise control flow. |
| Exceptions models the runtime/host/application failure domain.  | Domain errors models the business domain failures. |

## Inspiration & Homage

There is only one way to truly build anything in life, and that is to always start on the shoulder of giants. And this little project is no exceptions. Here are some of the languages, tools and learnings which are inspiring.

- The way C/Go handles application errors.
- The 'Either' monad.
- Functional style of control flow.
- The way Kotlin handles `null`.
- Kotlin's extension functions.
- The concept of a domain boundary.
- The ongoing pain of using Java.
- The ongoing joy of using the JVM platform.
- The million dollar mistake of introducing `null`

## Project Structure

The project consists of two modules:

| Module                            | Description                                                                                                |
| --------------------------------- | ---------------------------------------------------------------------------------------------------------- |
| [Core](core/README.md)            | Core `Result` abstractions based on the _'Either'_ monad with builtin functional flow API.                 |
| [Modelling](modelling/README.md)) | Based on  the core, but focus on implementing proper domain errors. Includes internationalization support. |

