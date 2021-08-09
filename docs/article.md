# Error Handling as a first class domain concern

- [Error Handling as a first class domain concern](#error-handling-as-a-first-class-domain-concern)
  - [Guidelines](#guidelines)
    - [Modelling guidelines](#modelling-guidelines)
    - [Implementation guidelines](#implementation-guidelines)
      - [Third party / integration guidelines](#third-party--integration-guidelines)
      - [Enterprise & other non functional crosscutting concerns](#enterprise--other-non-functional-crosscutting-concerns)

-------

Why is error handling an issue in JVM land? I mean, is it actually an issue?

This library was born out of some observations and frustrations  while developing enterprise JVM based applications. So while looking for something which is more than just a set of conventions, I decided to codify what I believe to be good practices into a library which purposefully steers a developer towards good practices, and at the same time also away from the business as usual model.

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

As a consequence consider the following:

1. Creating an exceptions may not be that expensive, but catching it is expensive. Usually this involves unrolling the call stack.
2. Throwing an exception also means that the code throwing the exception looses all flow control. Nothing wrong with this if this the intention.
3. Catching all exceptions leads to subtle errors which can sometime be hard to pin down due to the following reasons:
    - More often than not, such caught exceptions are far removed from the offending code/cause. Consequently, a developer has to spend much effort to track and understand the error handing code which is sometimes located deep in the bowls of a many nested levels of `try-catch` statements.
    - Sometimes an application would catch an exception which should never be handled under a `try-catch-all` statement, for example an `OutOfMemoryException`. If a developer forgets to log the error, the actual cause can sometimes just disappear leading to many man-hours hunting for something which should have been easy to fix: For example, give the process more memory, or finding the data structure leaking the memory.
4. The very act of raising an exception also has some serious untended consequences insofar as the domain driven design/modelling:
    - Causes the boundaries of one domain to flow into with another,
    - Each time such a "domain exception" is thrown (thus the lost control of flow in the domain throwing it), will almost certainly result in all other domains models to be invalidated.
    - Ultimately this would mean that each domain has to have deep knowledge of almost all errors on every other domain in the application.
    - Clearly it is almost impossible to design for this, as each exception thrown is like bullet punching holes in any well crafted domain boundary.
5. Lastly, Java's unfortunate decision to have checked exceptions just compounds these problems by encouraging developers to wrap checked exceptions into runtime exceptions. Most the time these wrapped exceptions has very little bearing on the domain and exists only because of the compiler forcing the developer to do so. On a practical level this has the consequence of hiding the underlying errors deep into logs and many-line stack traces.

> Note: ❗️ Exceptions are not undesirable, as long as they are used as intended.

## Guidelines

It is important not to loose sight of the overall objectives here insofar as the modelling and implementing domain errors:

1. There should be (where possible), an almost one-to-one translation to implementation.
2. A domain exceptions primary concern is the business process, specifically either a family of uses case, but ideally a single use case.
3. That being so, failure to handle such an error in the domain should result in a application level exception to indicate that implementation is not complete.
4. Many times, if not most of the time, these domain errors are should produce also customer friendly messages.

### Modelling guidelines

| #   | Guideline                                                     | Motivation                                                                                             |
| --- | ------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------ |
| 1.  | Do have a high level document describing the domain boundary. | Easy reasoning when something should be modelled as Domain Error vs Application exceptions.            |
| 2.  | Naming domain errors (codes) is important.                    | Easy and visually identifying meaningful errors in logs and relate it back to the the business domain. |
| 2.1 | Choose domain specific descriptive codes.                     | Conveying meaning and context up front.                                                                |
| 2.2 | Avoid numeric codes.                                          | Clearly indicating which alternate path needs to be followed in the use case.                          |
| 3.  | Keep a living document per domain of domain errors.           | Combats domain rot.                                                                                    |
| 4.  | Identify cross cutting concerns.                              | Prevents infrastructure/application related failures bleeding into the domain model.                   |
| 5.  | Always model domain errors in the context of a use case.      | Prevents the proliferation of errors which are not domain errors.                                      |
| 6.  | Identify the client/audience of your domain error             | Keeps domain error codes (and messages) relevant to the overall business objectives.                   |

> Here are is a example of some proper domain errors (taken from the [Stripe Payments processing](https://stripe.com/docs/api/errors) platform):
>
>| Error Code                        | Description                                                                                                                                            |
>| --------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------ |
>| `account_country_invalid_address` | The country of the business address provided does not match the country of the account. Businesses must be located in the same country as the account. |
>| `acss_debit_session_incomplete`   | The ACSS debit session is not ready to transition to complete status yet. Please try again the request later.                                          |

### Implementation guidelines

| #   | Guideline                                                                                                | Motivation                                                                                                                                                                               |
| --- | -------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1   | Ensure sure that all messages are produced via resource bundles.                                         | Domain errors should have meaningfully client facing content.                                                                                                                            |
| 2   | Ensure that each each domain error type is also able to produce developer/operational specific messages. | Developer messages most of the time serves a different purpose, and may contain information which describes a bit more that what is required for business and/or client facing messages. |
| 3   | Prefer sealed/case classes over enums                                                                    | Sealed (Kotlin) and Case Classes (Scala) represents a finite/fix number of errors, e.g error codes, but can also carry additional state.                                                 |
| 4   | Prefer enums over untyped constants.                                                                     | Enums are typed, and is a very natural fit for errors. Since the are also classes, they can be co-opted to carry more information (if such information is static by nature.)             |
| 5   | Ensure that any unhandled domain error results in actual exception being thrown.                         | Doing this indicates that an implementation is not complete. Remember domain errors are domain specific control flow advice which is suppose to mirror business/domain uses cases.       |

#### Third party / integration guidelines

| #   | Guideline                                                                                                                | Motivation                                                                                                                                     |
| --- | ------------------------------------------------------------------------------------------------------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------- |
| 1   | Treat 3rd party integration failures as infrastructure failures, e.g application exceptions.                             | The application should have already a defined domain. And 3rd party/integration failures are per definition is outside of the domain boundary. |
| 2   | Never use a 3rd party error as is, even it fits into the domain.                                                         | Limit, and control the effect of change in a 3rd party on your own domain.                                                                     |
| 3   | Implement a translation layer in the case of a 1:1 mapping between your domain error requirement and the 3d party error. | Limit, and control the effect of change in a 3rd party on your own domain.                                                                     |

#### Enterprise & other non functional crosscutting concerns
