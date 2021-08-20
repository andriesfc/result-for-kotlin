package resultk.modelling.testing.fixtures

import resultk.modelling.error.AbstractDomainError
import resultk.modelling.error.DomainError
import resultk.modelling.i8n.messagesBundle
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*


data class User(
    val name: String,
    val joinedDate: LocalDate,
    val lastNote: Note? = null
) {
    val joinedDatedAsText: String =
        joinedDate.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy"))
}


data class Note(
    val text: String,
    val dateCreated: LocalDateTime
)

data class Quote(
    val text: String,
    val attribution: String
)


enum class StripeError(override val errorCode: String) : DomainError {

    ACCOUNT_COUNTRY_INVALID_ADDRESS("account_country_invalid_address"),
    ACCOUNT_INVALID("account_invalid"),
    ACCOUNT_NUMBER_INVALID("account_number_invalid"),
    ACSS_DEBIT_SESSION_INCOMPLETE("acss_debit_session_incomplete"),
    ALIPAY_UPGRADE_REQUIRED("alipay_upgrade_required"),
    AMOUNT_TOO_LARGE("amount_too_large"),
    AMOUNT_TOO_SMALL("amount_too_small"),
    API_KEY_EXPIRED("api_key_expired");

    override val debugMessage: String? get() = mb["$errorCode.debug"]
    override val message: String get() = mb.message(errorCode)

    companion object {
        private val mb =
            messagesBundle("resultk/modelling/testing/fixtures/StripeTransactionError")
    }
}

sealed class StripeTransactionError(final override val errorCode: String, debugErrorCode: String) :
    AbstractDomainError(
        errorCode,
        debugErrorCode,
        mb
    ) {

    class AccountCountryInvalidAddress(
        override val traceId: String,
        override val date: LocalDateTime,
    ) : StripeTransactionError(
        "account_country_invalid_address",
        "account_country_invalid_address.debug"
    )

    class InvalidAccount(
        override val traceId: String,
        override val date: LocalDateTime,
    ) : StripeTransactionError("account_invalid", "account_invalid.debug")

    class AccountNumberInvalid(
        override val traceId: String,
        override val date: LocalDateTime,
    ) : StripeTransactionError("account_number_invalid", "account_number_invalid.debug")

    abstract val traceId: String
    abstract val date: LocalDateTime

    override fun toString(): String = buildString {
        append("[")
        append(errorCode)
        append("]: ")
        append(debugMessage ?: message)
    }

    override fun hashCode(): Int {
        return Objects.hash(errorCode, traceId, date, debugMessage)
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other === this) return true
        if (other !is StripeTransactionError) return false
        return errorCode == other.errorCode
                && traceId == other.traceId
                && date == other.date
                && debugMessage == other.debugMessage
    }

    companion object {
        private val mb =
            messagesBundle("resultk/modelling/testing/fixtures/StripeTransactionError")
    }
}