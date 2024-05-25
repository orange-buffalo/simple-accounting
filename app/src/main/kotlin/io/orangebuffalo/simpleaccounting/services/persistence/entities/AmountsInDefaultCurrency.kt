package io.orangebuffalo.simpleaccounting.services.persistence.entities

data class AmountsInDefaultCurrency(

    /**
     * The original amount after currency conversion to the default currency.
     * If entity is already in default currency, expected to be the same as its original amount.
     */
    var originalAmountInDefaultCurrency: Long?,

    /**
     * Amount in default currency after all adjustments (partial business purpose, general tax, etc)
     * are applied. Is expected to be used for reporting purposes as the final amount related to the
     * entity (for instance, to be reported for income tax purposes to the Tax Office).
     */
    var adjustedAmountInDefaultCurrency: Long?

) {

    /**
     * In case of the default currency, this shortcut constructor can be used.
     */
    constructor(amountInDefaultCurrency: Long?) : this(amountInDefaultCurrency, amountInDefaultCurrency)

    val empty: Boolean
        get() = originalAmountInDefaultCurrency == null && adjustedAmountInDefaultCurrency == null

    val notEmpty: Boolean
        get() = !empty
}
