package io.orangebuffalo.simpleaccounting.business.users.migration

import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class AdminUserRecoveryServiceTest(
    @Autowired private val adminUserRecoveryService: AdminUserRecoveryService,
) : SaIntegrationTestBase() {

    @Test
    fun `should create admin user on empty database`() {
        // TestDataExtension removes all data before the test, hence need to execute it one more time
        adminUserRecoveryService.recoverAdminUser()

        val allUsers = aggregateTemplate.findAll(PlatformUser::class.java).toList()
        allUsers.shouldHaveSize(1)
        allUsers.first().should {
            it.isAdmin.shouldBe(true)
            it.userName.shouldBe("admin")
            it.passwordHash.shouldStartWith("{noop}")
        }
    }

    @Test
    fun `should not create admin user if it already exists`() {
        preconditions {
            platformUser(userName = "test-admin", isAdmin = true, passwordHash = "test-password")
        }

        adminUserRecoveryService.recoverAdminUser()

        val allUsers = aggregateTemplate.findAll(PlatformUser::class.java).toList()
        allUsers.shouldHaveSize(1)
        allUsers.first().should {
            it.isAdmin.shouldBe(true)
            it.userName.shouldBe("test-admin")
            it.passwordHash.shouldBe("test-password")
        }
    }

    @Test
    fun `should create admin user if regular user exists`() {
        preconditions {
            platformUser(userName = "test-user", isAdmin = false)
        }

        adminUserRecoveryService.recoverAdminUser()

        val allUsers = aggregateTemplate.findAll(PlatformUser::class.java).toList()
        allUsers.shouldHaveSize(2)
        allUsers.map { it.userName }.shouldContainInOrder("test-user", "admin")
    }
}
