package io.orangebuffalo.simpleaccounting.services.persistence.repos

import io.orangebuffalo.simpleaccounting.junit.TestData
import io.orangebuffalo.simpleaccounting.junit.TestDataExtension
import io.orangebuffalo.simpleaccounting.Prototypes
import io.orangebuffalo.simpleaccounting.MOCK_TIME
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import javax.persistence.EntityManager

@ExtendWith(SpringExtension::class, TestDataExtension::class)
@SpringBootTest
@TestPropertySource(properties = ["logging.level.org.hibernate.SQL=DEBUG"])
class TimestampsIT(
    @Autowired val documentRepository: DocumentRepository,
    @Autowired val entityManager: EntityManager
) {

    @Test
    fun `Should store and load timestamp with time zone`(testData: TimestampsTestData) {
        entityManager.clear()
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
