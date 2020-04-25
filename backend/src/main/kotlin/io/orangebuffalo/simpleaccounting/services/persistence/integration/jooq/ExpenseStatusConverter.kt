package io.orangebuffalo.simpleaccounting.services.persistence.integration.jooq

import io.orangebuffalo.simpleaccounting.services.persistence.entities.ExpenseStatus
import org.jooq.Converter

class ExpenseStatusConverter : Converter<String, ExpenseStatus> {
    override fun from(databaseObject: String?): ExpenseStatus? =
        ExpenseStatus.values().firstOrNull { it.name == databaseObject }

    override fun to(userObject: ExpenseStatus?): String? = userObject?.name

    override fun fromType(): Class<String> = String::class.java

    override fun toType(): Class<ExpenseStatus> = ExpenseStatus::class.java
}
