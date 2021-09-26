package resultk.playgroundapp

fun main() {
}

private typealias Recording = MutableMap<String, MutableMap<String, String?>>

fun Map<String, String?>.records(recordPrefix: String): Map<String, Map<String, String?>> {

    if (isEmpty()) {
        return emptyMap()
    }

    val prefix = if (recordPrefix.endsWith("."))
        recordPrefix
    else "%s.".format(recordPrefix)

    val collected: Recording = entries.map { (k, v) ->
        k to v.takeUnless(String?::isNullOrEmpty)
    }.fold(mutableMapOf()) { collecting, (path, value) ->
        if (path.startsWith(prefix) && path.length > prefix.length) {

            val parentStart = prefix.length
            val parentEnd = path.indexOf('.', startIndex = parentStart + 1)
            val parent = if (parentEnd == -1) path.substring(parentStart) else path.substring(
                parentStart,
                parentEnd
            )

            val record = collecting.computeIfAbsent(parent) { mutableMapOf() }

            if (parentEnd != -1) {
                val field = path.substring(parentEnd + 1)
                record[field] = value
            }
        }
        collecting
    }

    return collected.toMap()
}

fun String.builder() = StringBuilder(this)

inline fun String.trimMargins(trimLeft: (Char) -> Boolean, trimRight: (Char) -> Boolean): String {

    if (isEmpty()) {
        return this
    }
    return builder().trimStart(trimLeft).trimEnd(trimRight).toString()
}