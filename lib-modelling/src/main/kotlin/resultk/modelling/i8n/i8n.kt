@file:JvmName("I8n")

package resultk.modelling.i8n

import resultk.internal.internalMessage
import resultk.modelling.internal.templating.ResolveExpression
import resultk.modelling.internal.templating.eval
import java.util.*

sealed class I8nError(private val errorKey: String) {

    abstract val locale: Locale

    data class MissingResourceBundle(
        val missingBasename: String,
        override val locale: Locale
    ) : I8nError("error.i8n.missingResourceBundle")

    data class MissingMessageKey(
        val baseName: String,
        val missingKey: String,
        override val locale: Locale,
    ) : I8nError("error.i8n.missingMessageKey")

    fun message() =
        internalMessage(errorKey)
            .eval(ResolveExpression.ByBeanModel(this))
            .get().toString()
}
