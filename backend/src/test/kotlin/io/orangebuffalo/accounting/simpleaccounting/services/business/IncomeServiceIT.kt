package io.orangebuffalo.accounting.simpleaccounting.services.business

import io.orangebuffalo.accounting.simpleaccounting.Prototypes
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.IncomeStatus
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityManager

@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
internal class IncomeServiceIT(
    @Autowired private val entityManager: EntityManager
) {

    @Test
    @Transactional
    fun `should fail when persisting invalid income`() {
        val income = Prototypes.income(
            status = IncomeStatus.FINALIZED,
            incomeTaxableAmounts = Prototypes.emptyAmountsInDefaultCurrency()
        )
        entityManager.persist(income.workspace.owner)
        entityManager.persist(income.workspace)
        entityManager.persist(income)

        Assertions.assertThatThrownBy { entityManager.flush() }
            .hasMessageContaining("Inconsistent income:")
    }

}

