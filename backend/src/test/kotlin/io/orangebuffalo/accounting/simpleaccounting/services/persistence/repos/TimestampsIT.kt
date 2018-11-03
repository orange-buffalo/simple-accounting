package io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos

import io.orangebuffalo.accounting.simpleaccounting.junit.TestDataExtension
import io.orangebuffalo.accounting.simpleaccounting.junit.testdata.Fry
import io.orangebuffalo.accounting.simpleaccounting.web.MOCK_TIME
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
    fun `Should store and load timestamp with time zone`(fry: Fry) {
        entityManager.clear()
        val documentFromDb = documentRepository.findById(fry.slurmReceipt.id!!)
        assertThat(documentFromDb).hasValueSatisfying {
            assertThat(it.timeUploaded).isEqualTo(MOCK_TIME)
        }
    }
}