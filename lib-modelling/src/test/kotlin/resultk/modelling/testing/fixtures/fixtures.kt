package resultk.modelling.testing.fixtures

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.reflect.KProperty1

data class UserBean(val name: String, val joinedDate: LocalDate, val lastNote: Note? = null) {
    val joinedDatedAsText: String get() = joinedDate.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy"))

    data class Note(val text: String, val dateCreated: LocalDateTime)

}

inline fun <reified T> T.mapped(prefix: String? = null): MutableMap<String, Any?> = (T::class.members)
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

