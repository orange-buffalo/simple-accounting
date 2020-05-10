package io.orangebuffalo.simpleaccounting.services.persistence.repos

import io.orangebuffalo.simpleaccounting.MOCK_TIME
import io.orangebuffalo.simpleaccounting.Prototypes
import io.orangebuffalo.simpleaccounting.junit.TestData
import io.orangebuffalo.simpleaccounting.junit.TestDataExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class, TestDataExtension::class)
@SpringBootTest
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
