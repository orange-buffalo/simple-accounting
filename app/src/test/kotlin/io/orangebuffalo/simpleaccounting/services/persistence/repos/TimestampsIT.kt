package io.orangebuffalo.simpleaccounting.services.persistence.repos

import io.orangebuffalo.simpleaccounting.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.infra.database.Prototypes
import io.orangebuffalo.simpleaccounting.domain.documents.DocumentRepository
import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.infra.database.TestDataDeprecated
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@SimpleAccountingIntegrationTest
class TimestampsIT(
    @Autowired val documentRepository: DocumentRepository
) {

    @Test
    fun `Should store and load timestamp with time zone`(testData: TimestampsTestData) {
        val documentFromDb = documentRepository.findById(testData.slurmReceipt.id!!)
        assertThat(documentFromDb).hasValueSatisfying {
            assertThat(it.timeUploaded).isEqualTo(MOCK_TIME)
        }
    }

    class TimestampsTestData : TestDataDeprecated {
        val fry = Prototypes.fry()
        val workspace = Prototypes.workspace(owner = fry)
        val slurmReceipt = Prototypes.document(workspace = workspace)

        override fun generateData() = listOf(fry, workspace, slurmReceipt)
    }
}
