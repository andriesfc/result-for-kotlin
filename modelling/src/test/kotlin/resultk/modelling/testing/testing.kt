package resultk.modelling.testing

import java.io.FileNotFoundException
import java.net.URL
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmName

object ClasspathResources {
    operator fun get(resource: String): URL {
        return javaClass.getResource(resource) ?: throw FileNotFoundException("classpath:$resource")
    }
}

fun resource(resource: String): Properties = ClasspathResources[resource].run {
    openStream().reader().use {
        Properties().apply {
            load(it)
        }
    }
}

fun Properties.includeKeys(test: (String) -> Boolean) =
    apply { stringPropertyNames().removeIf(test) }

fun Properties.mapped(): Map<String, String> = stringPropertyNames().associateBy(this::getProperty)

private fun throwNoKeyIn(key: Any?, inContainer: Any): Nothing =
    throw NoSuchElementException("No key found: $key in [$inContainer]")

fun <K, V> Map<K, V>.require(key: K): V = get(key) ?: throwNoKeyIn(key, this)

fun Properties.require(key: String): String = getProperty(key) ?: throwNoKeyIn(key, this)

fun String.build(buildThis: StringBuilder.() -> Unit) = buildString {
    append(this@build)
    buildThis()
}

fun <T : Any> KClass<T>.new(vararg namedArgs: Pair<String, Any?>): T {
    val cons = requireNotNull(primaryConstructor) { "No primary constructor found for $jvmName" }
    val params = namedArgs.toMap().run { cons.parameters.associateWith { get(it.name) } }
    return cons.callBy(params)
}

fun LocalDateTime.atTime(time: LocalTime): LocalDateTime = toLocalDate().atTime(time)
fun LocalDateTime.atTimeNow(): LocalDateTime = atTime(LocalTime.now())