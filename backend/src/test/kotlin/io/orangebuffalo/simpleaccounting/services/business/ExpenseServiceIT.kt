package io.orangebuffalo.simpleaccounting.services.business

import io.orangebuffalo.simpleaccounting.Prototypes
import io.orangebuffalo.simpleaccounting.services.persistence.entities.ExpenseStatus
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
internal class ExpenseServiceIT(
    @Autowired private val entityManager: EntityManager
) {

    @Test
    @Transactional
    fun `should fail when persisting invalid expense`() {
        val expense = Prototypes.expense(
            status = ExpenseStatus.FINALIZED,
            incomeTaxableAmounts = Prototypes.emptyAmountsInDefaultCurrency()
        )
        entityManager.persist(expense.workspace.owner)
        entityManager.persist(expense.workspace)
        entityManager.persist(expense)

        Assertions.assertThatThrownBy { entityManager.flush() }
            .hasMessageContaining("Inconsistent expense:")
    }

}

