package io.orangebuffalo.accounting.simpleaccounting.junit.testdata

import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Category
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.PlatformUser
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Tax
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace

class Prototypes {
    companion object {
        fun fry() = platformUser(
            userName = "Fry",
            passwordHash = "qwertyHash",
            isAdmin = false
        )

        fun farnsworth() = platformUser(
            userName = "Farnsworth",
            passwordHash = "scienceBasedHash",
            isAdmin = true
        )

        fun zoidberg() = platformUser(
            userName = "Zoidberg",
            passwordHash = "??",
            isAdmin = false
        )

        fun bender() = platformUser(
            userName = "Bender",
            passwordHash = "011101010101101001",
            isAdmin = false
        )

        fun platformUser(
            userName: String = "noname",
            passwordHash: String = "nopassword",
            isAdmin: Boolean = false
        ) = PlatformUser(
            userName = userName,
            passwordHash = passwordHash,
            isAdmin = isAdmin
        )

        fun workspace(
            name: String = "workspace",
            owner: PlatformUser = platformUser(),
            taxEnabled: Boolean = true,
            multiCurrencyEnabled: Boolean = true,
            defaultCurrency: String = "AUD",
            categories: MutableList<Category> = ArrayList()
        ) = Workspace(
            name = name,
            owner = owner,
            taxEnabled = taxEnabled,
            multiCurrencyEnabled = multiCurrencyEnabled,
            defaultCurrency = defaultCurrency,
            categories = categories
        )

        fun tax(
            title: String = "tax",
            rateInBps: Int = 10_00,
            description: String? = null,
            workspace: Workspace = workspace()
        ) = Tax(
            title = title,
            workspace = workspace,
            rateInBps = rateInBps,
            description = description
        )

        fun category(
            name: String = "category",
            description: String? = null,
            workspace: Workspace = workspace(),
            income: Boolean = true,
            expense: Boolean = true
        ) = Category(
            name = name,
            workspace = workspace,
            income = income,
            expense = expense,
            description = description
        )
    }
}