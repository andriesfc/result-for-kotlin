package resultk.internal

import java.util.ResourceBundle.getBundle

internal const val INTERNAL_MESSAGE_RESOURCE_BUNDLE = "resultk/internal/messages"

fun internalMessage(key: String, vararg args: Any?): String {
    return getBundle(INTERNAL_MESSAGE_RESOURCE_BUNDLE).getString(key).let { message ->
        when {
            args.isEmpty() -> message
            else -> message.format(* args)
        }
    }
}