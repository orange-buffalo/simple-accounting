package io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos

import io.orangebuffalo.accounting.simpleaccounting.services.persistence.entities.PlatformUser
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
class PlatformUserRepositoryTest {

    @Autowired
    lateinit var entityManager: EntityManager

    @Autowired
    lateinit var platformUserRepository: PlatformUserRepository

    @Test
    fun `Should load an entity saved by entity manager`() {
        val platformUser = PlatformUser(
                userName = "Fry",
                passwordHash = "qwerty",
                isAdmin = false
        )
        entityManager.persist(platformUser)
        entityManager.flush()
        entityManager.clear()

        val users = platformUserRepository.findAll()
        assertThat(users)
            .hasSize(1)
            .allSatisfy { actualUser ->
                assertThat(actualUser.id).isEqualTo(platformUser.id)
                assertThat(actualUser.version).isEqualTo(0)
                assertThat(actualUser.userName).isEqualTo("Fry")
                assertThat(actualUser.passwordHash).isEqualTo("qwerty")
                assertThat(actualUser.isAdmin).isEqualTo(false)
            }
    }
}