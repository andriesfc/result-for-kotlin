@file:Suppress("ReplaceGetOrSet")

package resultk.modelling.error

import resultk.modelling.i8n.I8nMessagesBundle
import resultk.value

interface Error {
    val errorCode: String
}

interface ErrorMessage {
    val message: String
    val debugMessage: String?
}

interface DomainError : Error, ErrorMessage {
    companion object
}

@Suppress("MemberVisibilityCanBePrivate")
abstract class AbstractDomainError(
    override val errorCode: String,
    val debugErrorCode: String,
    protected val bundle: I8nMessagesBundle
) : DomainError {
    final override val debugMessage: String?
        get() {
            return when (debugErrorCode) {
                in bundle -> debug()
                else -> null
            }
        }
    final override val message: String get() = error()
    protected open fun debug() = bundle.messageWithBean(debugErrorCode, this).value
    protected open fun error() = bundle.message(errorCode)
}