package io.orangebuffalo.simpleaccounting.services.persistence.entities

import java.io.Serializable
import javax.persistence.Column
import javax.persistence.Embeddable

/**
 * Aggregator over a set of same-purpose amounts (i.e. amounts for taxation purposes, bank transaction amounts, etc).
 */
@Embeddable
data class AmountsInDefaultCurrency(

    /**
     * The original amount after currency conversion to the default currency.
     * If entity is already in default currency, expected to be the same as its original amount.
     */
    @field:Column
    var originalAmountInDefaultCurrency: Long?,

    /**
     * Amount in default currency after all adjustments (partial business purpose, general tax, etc)
     * are applied. Is expected to be used for reporting purposes as the final amount related to the
     * entity (for instance, to be reported for income tax purposes to the Tax Office).
     */
    @field:Column
    var adjustedAmountInDefaultCurrency: Long?

) : Serializable {

    val empty: Boolean
        get() = originalAmountInDefaultCurrency == null && adjustedAmountInDefaultCurrency == null

    val notEmpty: Boolean
        get() = !empty
}
