package resultk.modelling.internal

/**
 * This error case deals with internal modelling errors. They are not supposed to
 * be passed on to public API, except as exception. This will ensure that all internal errors
 * are either handled or result in failure.
 *
 * @property errorKey The errorKey used to construct a message with.
 * @constructor
 */
sealed class InternalModellingError(val errorKey: String) {

    data class MissingMessageExpressions(val template: String, val expressions: List<String>) :
        InternalModellingError("error.templating.missingMessagePlaceholder")

    data class UnexpectedFailure(val cause: Throwable) :
        InternalModellingError("error.templating.unexpectedFailure")

    data class MalformedTemplate(
        val index: Int,
        val template: String,
        val reportedVia: Any,
        val cause: Throwable?
    ) : InternalModellingError("error.templating.malformedTemplateEncountered")
}
