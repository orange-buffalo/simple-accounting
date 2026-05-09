package io.orangebuffalo.simpleaccounting.business.generaltaxes

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntity
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

/**
 * [General tax](https://en.wikipedia.org/wiki/List_of_taxes) includes taxes that
 * are typically added into the income / expense amounts, for example Value Added Tax,
 * Sales Tax, etc. They are processed separately and not included into the income tax.
 */
@Table
data class GeneralTax(
    val title: String,
    val rateInBps: Int,
    val description: String? = null,
    val workspaceId: String,
    override val id: String? = null,
    override val version: Int? = null,
    override val createdAt: Instant? = null,
) : AbstractEntity()
