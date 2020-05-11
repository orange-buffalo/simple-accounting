package io.orangebuffalo.simpleaccounting.services.persistence.repos

import io.orangebuffalo.simpleaccounting.MOCK_TIME
import io.orangebuffalo.simpleaccounting.Prototypes
import io.orangebuffalo.simpleaccounting.junit.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.junit.TestData
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

    class TimestampsTestData : TestData {
        val fry = Prototypes.fry()
        val workspace = Prototypes.workspace(owner = fry)
        val slurmReceipt = Prototypes.document(workspace = workspace)

        override fun generateData() = listOf(fry, workspace, slurmReceipt)
    }
}
