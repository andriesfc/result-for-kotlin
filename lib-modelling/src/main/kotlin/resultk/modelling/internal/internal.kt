package resultk.modelling.internal

import resultk.DefaultFailureUnwrappingException
import resultk.Result.Failure
import resultk.internal.internalMessage
import resultk.map
import resultk.modelling.internal.templating.ResolveExpression
import resultk.modelling.internal.templating.eval

/**
 * This error case deals with internal modelling errors. They are not supposed to
 * be passed on to public API, except as exceptions. This will ensure that all internal errors
 * are either handled or resulting in a failure.
 *
 * @property errorKey The errorKey used to construct a message with.
 * @constructor
 */
sealed class InternalModellingError(val errorKey: String) : resultk.ThrowableProvider<Throwable> {

    data class UnresolvedTemplateExpression(val template: String, val expressions: List<String>) :
        InternalModellingError("error.templating.unresolvedTemplateExpression")

    data class UnexpectedFailure(val cause: Throwable) :
        InternalModellingError("error.templating.unexpectedFailure") {
        override fun throwing(): Throwable = cause
    }

    data class MalformedTemplate(
        val index: Int,
        val template: String,
        val reportedVia: Any,
        val cause: Throwable?
    ) : InternalModellingError("error.templating.malformedTemplateEncountered") {
        override fun throwing(): Throwable = cause ?: super.throwing()
    }

    override fun throwing(): Throwable = DefaultFailureUnwrappingException(Failure(this))

    fun message(): String = internalMessage(errorKey)
        .eval(ResolveExpression.ByBeanModel(this))
        .map(StringBuilder::toString).get()
}
