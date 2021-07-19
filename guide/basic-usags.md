# Basic Usage

Here are some examples:

## Simple Example

The first is a very simple use case. Essentially returning the bytes in a file, or a `IOException` if the file cannot be read.

```kotlin
// Return bytes in a file, or capture the I/O exception in the case of read failure
val bytes = resultOf<IOException,ByteArray> { file.readBytes().success() }
```

## A more realistic sample

>The 2nd example is a bit more involved. Here is a bit of context: 

The team is task to implemented a media conversion function for files as part of a product commercial product with several well known clients already lined up for pre release alpha, and some commercial deals are already in the in the pipeline for version 1.0.0 of the product.

### Our Requirement

The team decided to use a commercial offering from a company called 'Med Corp' based partially on a prior commercial arrangement, and quality and wide range conversion tools already used internally by the same the same company.

Unfortunately the team cannot use Med Con's internal errors as is in our initial product offering for the following reasons:

1. Even while the initial MVP will use Med Con's library, we are planning to bring in or replace this library either with some in house version, or another with another provider.
2. The error messages provided are very cryptic, and not very helpful with out some context.
3. The nature of product requires us to known what part of the encoding fail in order fulfil the required business flow.

Based on the last workshop, the team suggest the following error conditions: 

- Invalid Media Format
- Empty Media, for example an empty File
- Missing Media
- Failure initialize the converter library.
- Failure while encoding the media.
- Any unexpected errors.

The product team is insist that we should provide meaningful error messages to users of this product. But we also do not want to make these messages opaque to developers using this conversion utility. Furthermore, some clients in the pipeline is French based. So we need to make sure that all errors produced can be localized. This include developer facing (as the product initial consumers will be our clients staff engineers), and of cause public facing.

### A Solution POC

We introduce the notion of `ConversionError`. Each conversion failure will be identified by an error code. This will allow us to not only to quickly identify such errors in stack traces, error messages etc, but more importantly allows to do fulfill two very important requirements:

1. It provides locale specific error messages which are client facing.
2. It provides locale specific error messages which are developer facing.

As we may need to enrich each message independently with additional data, such the file which not be read, we opted to go with a sealed hierarchy of classes and instances. This means we can constant messages, as well as messages capturing runtime information.

It uses a `ConversionError` sealed class to represents specific failures:

| Error Code                                        | Error Type          | Purpose                                                                       |
| ------------------------------------------------- | ------------------- | ----------------------------------------------------------------------------- |
| `error.media_conversion.invalid_input_format`     | `InvalidInputMedia` | Indicates the media is not suited for conversion.                             |
| `error.media_conversion.conversion.nothing_to_do` | `NothingToDo`       | Indicates the media is empty, so there is nothing to do.                      |
| `error.media_conversion.unexpected_failure `      | `UnexpectedFailure` | Something unrelated to the encoder happened, which could not be handled.      |
| `error.media_conversion.encoder_init_failed`      | `EncoderInitError`  | The media converter could not be properly initialized.                        |
| `error.media_conversion.encoding_failed`          | `EncodingFailed`    | Encoder reported the encoding of the media failed.                            |
| `error.media_conversion.media_not_available`      | `MediaNotAvailable` | The media to encode could not be found, for example the file does not exists. |

These errors are represented as sealed classes, instead of just static error codes and/or `enums`. This implementation offers several advantages:

1. Each `ConversionError` also has static error code, this is used to quickly identify the error on sight.
2. Some like `ConversionError.NothingToDo` are just like a `enum`, as there is only one instance.
3. Some are full blown class instances which report, (depending on the actual error) different values, for example:
4. Each of these `ConversionError` instances also have proper message sourced from message bundle based on the errors values captured and the error code. 

```kotlin

import corp.abc.medcon.converter.MediaConverter
import corp.abc.medcon.converter.MediaConverterException

// Given the following error states: 
sealed class ConversionError(val code: String) {

    open fun message(): String {
        return MessageBundle.getBundle("Messages").getString(code)
    }
    
    object InvalidInputMedia: ConversionError("error.media_conversion.invalid_input_format")
    object NothingToDo: ConversionError("error.media_conversion.conversion.nothing_to_do")
    
    data class UnexpectedFailure(val cause: Exception)
        : ConversionError("error.media_conversion.unexpected_failure")
    
    data class EncoderInitError(val cause: MediaConverterException)
        : ConversionError("error.media_conversion.encoder_init_failed") {
            override fun message() = super.message().format(cause.message)
        }
    
    data class EncodingFailed(val source: Encoder, val cause: MediaConverterException)
        : ConversionError("error.media_conversion.encoding_failed") {
            override fun message() = super.message().format(source, cause.message)
        }   
        
    data class MediaNotAvailable<out T>(val media: T)
        : ConversionError("error.media_conversion.media_not_available") {
            override fun message(): String = super.message().format(media)
        }
}

// A function which converts file content 
fun convert(file: File, converter: MediaConverter): Result<ConversionError,String> {
    return resultOf {
        try {
            val fileLength = file.length()
            if (fileLength == 0) {
                ConversionError.NothingToDo.failure()
            } else if (!file.isFile()) {
                ConversionError.InvalidInputMedia.failure()
            } else {
                converter.init()
                file.forEachBlock { out, bytes -> encoder.update(out, 0, bytes) }
                converter.finish().success()    
            }
        } catch (e: MediaEncoderException) {
            when (e) {
                is MalfomedMediaException -> ConversionError.InvalidInputFormat
                is MediaConverterInitFailedException -> ConversionError.EncoderInitError(cause)
                else -> ConversionError.UnexpectedFailure(converter, e)
            }.failure()
        } catch (e: FileNotFoundException) {
            ConversionError.MediaNotAvailable(file).failure()
        } catch (e: Exception) {
            ConversionError.UnexpectedFailure(e).failure()
        }
    }
}
```




