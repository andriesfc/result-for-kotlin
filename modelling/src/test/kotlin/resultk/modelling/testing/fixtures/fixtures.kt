package resultk.modelling.testing.fixtures

import resultk.modelling.error.Error
import resultk.modelling.i8n.I8nMessages
import resultk.modelling.i8n.keyBundle
import resultk.modelling.i8n.required
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.reflect.KProperty1

data class UserBean(val name: String, val joinedDate: LocalDate, val lastNote: Note? = null) {
    val joinedDatedAsText: String get() = joinedDate.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy"))

    data class Note(val text: String, val dateCreated: LocalDateTime)

}

inline fun <reified T> T.mapped(prefix: String? = null): MutableMap<String, Any?> =
    (T::class.members)
        .filterIsInstance<KProperty1<T, *>>()
        .associate { property ->
            property.name.let { name ->
                when (prefix) {
                    null -> name
                    else -> "$prefix.$name"
                }
            } to property.get(this)
        }.toMutableMap()

data class Quote(
    val text: String,
    val attribution: String
)

interface StripeError : Error {
    companion object {
        val keyBundle: I8nMessages.KeyBundle = keyBundle(
            "resultk/modelling/testing/fixtures/StripeTransactionError"
        ).required()
    }
}

enum class StripeTransactionErrorEnum(override val errorCode: String) : StripeError {
    ACCOUNT_COUNTRY_INVALID_ADDRESS("account_country_invalid_address"),
    ACCOUNT_INVALID("account_invalid"),
    ACCOUNT_NUMBER_INVALID("account_number_invalid"),
    ACSS_DEBIT_SESSION_INCOMPLETE("acss_debit_session_incomplete"),
    ALIPAY_UPGRADE_REQUIRED("alipay_upgrade_required"),
    AMOUNT_TOO_LARGE("amount_too_large"),
    AMOUNT_TOO_SMALL("amount_too_small"),
    API_KEY_EXPIRED("api_key_expired")
}

