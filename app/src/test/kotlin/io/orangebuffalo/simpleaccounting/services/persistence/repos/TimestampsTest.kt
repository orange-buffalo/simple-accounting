package io.orangebuffalo.simpleaccounting.services.persistence.repos

import io.orangebuffalo.simpleaccounting.business.documents.DocumentsRepository
import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.infra.database.PreconditionsFactory
import io.orangebuffalo.simpleaccounting.infra.utils.MOCK_TIME
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@SimpleAccountingIntegrationTest
class TimestampsTest(
    @Autowired private val documentRepository: DocumentsRepository,
    preconditionsFactory: PreconditionsFactory,
) {

    @Test
    fun `should store and load timestamp with time zone`() {
        val documentFromDb = documentRepository.findById(preconditions.slurmReceipt.id!!)
        assertThat(documentFromDb).hasValueSatisfying {
            assertThat(it.timeUploaded).isEqualTo(MOCK_TIME)
        }
    }

    private val preconditions by preconditionsFactory {
        object {
            val fry = fry()
            val workspace = workspace(owner = fry)
            val slurmReceipt = document(workspace = workspace)
        }
    }
}
