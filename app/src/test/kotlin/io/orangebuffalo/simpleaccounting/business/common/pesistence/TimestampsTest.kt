package io.orangebuffalo.simpleaccounting.business.common.pesistence

import io.orangebuffalo.simpleaccounting.business.documents.DocumentsRepository
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
class TimestampsTest(
    @Autowired private val documentRepository: DocumentsRepository
) : SaIntegrationTestBase() {

    @Test
    fun `should store and load timestamp with time zone`() {
        val documentFromDb = documentRepository.findById(preconditions.slurmReceipt.id!!)
        assertThat(documentFromDb).hasValueSatisfying {
            assertThat(it.timeUploaded).isEqualTo(MOCK_TIME)
        }
    }

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry()
            val workspace = workspace(owner = fry)
            val slurmReceipt = document(workspace = workspace)
        }
    }
}
