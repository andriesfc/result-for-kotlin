package resultk.internal

import java.util.ResourceBundle.getBundle

internal const val MESSAGE_RESOURCE_BUNDLE = "resultk/internal/messages"

fun resourceMessage(key: String, vararg args: Any?): String {
    return getBundle(MESSAGE_RESOURCE_BUNDLE).getString(key).let { message ->
        when {
            args.isEmpty() -> message
            else -> message.format(* args)
        }
    }
}