package resultk.modelling.internal.templating.fixture.testcasemodel

import java.time.LocalDate
import java.time.Month
import java.util.*
import kotlin.reflect.full.memberProperties

data class TestCaseModel(val today: LocalDate, val kind: String, val n: Int) {
    constructor() : this(
        today = LocalDate.of(2021, Month.MARCH, 12),
        kind = "simple",
        n = 12
    )

    companion object {
        val default = TestCaseModel()
    }
}


fun TestCaseModel.mapped() =
    TestCaseModel::class.memberProperties.associate { p -> p.name to p.get(this) }

fun TestCaseModel.mappedByJavaProps() = Properties().also { properties ->
    mapped().forEach { (k, v) ->
        properties.setProperty(
            k,
            v.toString()
        )
    }
}
