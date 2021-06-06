package io.github.andriesfc.resultk.demo.taxcalc

import io.github.andriesfc.resultk.Result
import java.math.BigInteger
import java.time.LocalDateTime
import java.util.*

interface TaxCalculationService {

    class CalculationError(
        val taxRef: String,
        val message: String,
        details: Map<Indicator, String>
    ) : Map<CalculationError.Indicator, String> by details {

        enum class Indicator {
            InvalidTaxCode,
            UnRegisteredTaxEntity,
            UnavailableTextCode,
            AmountNotInRange,
            TechnicalError,
        }

        override fun equals(other: Any?): Boolean {
            return when {
                other === this -> true
                other is CalculationError -> (other.taxRef == taxRef)
                        && (other.message == message)
                        && (other.size == size)
                        && entries.containsAll(other.entries)
                else -> false
            }
        }

        override fun hashCode(): Int {
            val header: List<Any> = listOf(taxRef, message)
            val body: List<Any> = entries.sortedBy { it.key }.toList().flatMap { (k, v) -> listOf(k, v) }
            return Objects.hash((header + body).toTypedArray())
        }

        override fun toString(): String {
            val tab = "  "
            return buildString {
                append(CalculationError::class.qualifiedName).appendLine(" {")
                appendLine("taxRef =  $taxRef;".prependIndent(tab))
                appendLine("message = $message;".prependIndent(tab))
                forEach { t, u -> appendLine("$t = $u;".prependIndent(tab)) }
                appendLine("}")
            }
        }
    }

    data class Income(val taxCode: String, val amount: BigInteger)
    data class Deductibles(val taxCode: String, val amount: BigInteger)
    data class TaxableEntity(val name: String, val taxNo: String)

    sealed class Amount {
        abstract val value: BigInteger

        data class Payable(val taxCode: String, override val value: BigInteger) : Amount()
        data class Refund(val refundRefNo: String, val taxCode: String, override val value: BigInteger) : Amount()
    }

    data class TaxCalculation(
        val ref: String,
        val taxDue: List<Amount.Payable>,
        val taxRefunds: List<Amount.Refund>,
        val dateTime: LocalDateTime,
    )

    fun calculateTax(
        taxableEntity: TaxableEntity,
        totalIncome: Set<Income>,
        deductibles: Set<Deductibles>,
    ): Result<CalculationError, TaxCalculation>

}

