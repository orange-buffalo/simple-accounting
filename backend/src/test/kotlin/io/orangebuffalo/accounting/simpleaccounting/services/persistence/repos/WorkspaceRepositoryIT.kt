package io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos

import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.PlatformUser
import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.Workspace
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityManager

@ExtendWith(SpringExtension::class)
@SpringBootTest
@Transactional
@TestPropertySource(properties = ["logging.level.org.hibernate.SQL=DEBUG"])
class WorkspaceRepositoryIT {

    @Autowired
    lateinit var entityManager: EntityManager

    @Autowired
    lateinit var workspaceRepository: WorkspaceRepository

    @Test
    fun `Should load an entity saved by entity manager`() {
        val platformUser = PlatformUser(
                userName = "Fry",
                passwordHash = "qwerty",
                isAdmin = false
        )
        val workspace = Workspace(
                name = "w1",
                owner = platformUser,
                defaultCurrency = "AUD",
                multiCurrencyEnabled = false,
                taxEnabled = true
        )
        entityManager.persist(platformUser)
        entityManager.persist(workspace)
        entityManager.flush()
        entityManager.clear()

        val workspaces = workspaceRepository.findAll()
        assertThat(workspaces)
            .hasSize(1)
            .allSatisfy { actualWorkspace ->
                assertThat(actualWorkspace.id).isEqualTo(workspace.id)
                assertThat(actualWorkspace.version).isEqualTo(0)
                assertThat(actualWorkspace.name).isEqualTo("w1")
                assertThat(actualWorkspace.owner).isEqualTo(platformUser)
                assertThat(actualWorkspace.defaultCurrency).isEqualTo("AUD")
                assertThat(actualWorkspace.multiCurrencyEnabled).isEqualTo(false)
                assertThat(actualWorkspace.taxEnabled).isEqualTo(true)
            }
    }
}