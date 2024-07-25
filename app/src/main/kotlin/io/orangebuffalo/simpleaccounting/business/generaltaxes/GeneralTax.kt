package io.orangebuffalo.simpleaccounting.business.generaltaxes

import io.orangebuffalo.simpleaccounting.business.common.pesistence.AbstractEntity
import org.springframework.data.relational.core.mapping.Table

/**
 * [General tax](https://en.wikipedia.org/wiki/List_of_taxes) includes taxes that
 * are typically added into the income / expense amounts, for example Value Added Tax,
 * Sales Tax, etc. They are processed separately and not included into the income tax.
 */
@Table
class GeneralTax(
    var title: String,
    var rateInBps: Int,
    var description: String? = null,
    val workspaceId: Long
) : AbstractEntity()
