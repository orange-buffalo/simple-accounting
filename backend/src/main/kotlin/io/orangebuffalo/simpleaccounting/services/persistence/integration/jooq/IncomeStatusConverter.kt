package io.orangebuffalo.simpleaccounting.services.persistence.integration.jooq

import io.orangebuffalo.simpleaccounting.services.persistence.entities.IncomeStatus
import org.jooq.Converter

class IncomeStatusConverter : Converter<String, IncomeStatus> {
    override fun from(databaseObject: String?): IncomeStatus? =
        IncomeStatus.values().firstOrNull { it.name == databaseObject }

    override fun to(userObject: IncomeStatus?): String? = userObject?.name

    override fun fromType(): Class<String> = String::class.java

    override fun toType(): Class<IncomeStatus> = IncomeStatus::class.java
}
