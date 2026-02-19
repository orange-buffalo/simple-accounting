package io.orangebuffalo.simpleaccounting.business.api

import io.kotest.matchers.equals.shouldBeEqual
import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import io.orangebuffalo.simpleaccounting.infra.graphql.client.MutationProjection
import io.orangebuffalo.simpleaccounting.tests.infra.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.api.GraphqlClientRequestExecutor
import io.orangebuffalo.simpleaccounting.tests.infra.api.expectThatJsonBodyEqualTo
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphqlMutation
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findAll
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeSingle
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired

class ChangePasswordMutationTest(
    @param:Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry()
            val farnsworth = farnsworth()
            val workspaceAccessToken = workspaceAccessToken()
        }
    }

    @Test
    fun `should change password for regular user`() {
        testSuccessPath(preconditions.fry)
    }

    @Test
    fun `should change password for admin user`() {
        testSuccessPath(preconditions.farnsworth)
    }

    @Test
    fun `should return NOT_AUTHORIZED error for anonymous requests`() {
        client
            .graphqlMutation { changePasswordMutation("current-password", "new-password") }
            .fromAnonymous()
            .executeAndVerifyNotAuthorized(
                path = "changePassword"
            )
    }

    @Test
    fun `should return NOT_AUTHORIZED error when changing password by a transient user`() {
        client
            .graphqlMutation { changePasswordMutation("current-password", "new-password") }
            .usingSharedWorkspaceToken(preconditions.workspaceAccessToken.token)
            .executeAndVerifyNotAuthorized(
                path = "changePassword"
            )
    }

    @Test
    fun `should return BUSINESS_ERROR with CURRENT_PASSWORD_MISMATCH when current password does not match`() {
        whenever(passwordEncoder.matches("wrong-password", preconditions.fry.passwordHash)) doReturn false

        client
            .graphqlMutation { changePasswordMutation("wrong-password", "new-password") }
            .from(preconditions.fry)
            .executeAndVerifyBusinessError(
                message = "Invalid current password",
                errorCode = "CURRENT_PASSWORD_MISMATCH",
                path = "changePassword"
            )
    }

    @Test
    fun `should return FIELD_VALIDATION_FAILURE when currentPassword is blank`() {
        client
            .graphqlMutation { changePasswordMutation("  ", "new-password") }
            .from(preconditions.fry)
            .executeAndVerifyValidationError(
                violationPath = "currentPassword",
                error = "MustNotBeBlank",
                message = "must not be blank",
                path = "changePassword"
            )
    }

    @Test
    fun `should return FIELD_VALIDATION_FAILURE when currentPassword is empty`() {
        client
            .graphqlMutation { changePasswordMutation("", "new-password") }
            .from(preconditions.fry)
            .executeAndVerifyValidationError(
                violationPath = "currentPassword",
                error = "MustNotBeBlank",
                message = "must not be blank",
                path = "changePassword"
            )
    }

    @Test
    fun `should return FIELD_VALIDATION_FAILURE when newPassword is blank`() {
        client
            .graphqlMutation { changePasswordMutation("current-password", "  ") }
            .from(preconditions.fry)
            .executeAndVerifyValidationError(
                violationPath = "newPassword",
                error = "MustNotBeBlank",
                message = "must not be blank",
                path = "changePassword"
            )
    }

    @Test
    fun `should return FIELD_VALIDATION_FAILURE when newPassword is empty`() {
        client
            .graphqlMutation { changePasswordMutation("current-password", "") }
            .from(preconditions.fry)
            .executeAndVerifyValidationError(
                violationPath = "newPassword",
                error = "MustNotBeBlank",
                message = "must not be blank",
                path = "changePassword"
            )
    }

    @Test
    fun `should return FIELD_VALIDATION_FAILURE when currentPassword exceeds max length`() {
        val tooLongPassword = "a".repeat(101)
        client
            .graphqlMutation { changePasswordMutation(tooLongPassword, "new-password") }
            .from(preconditions.fry)
            .executeAndVerifyValidationError(
                violationPath = "currentPassword",
                error = "SizeConstraintViolated",
                message = "size must be between 0 and 100",
                path = "changePassword",
                params = mapOf("min" to "0", "max" to "100")
            )
    }

    @Test
    fun `should return FIELD_VALIDATION_FAILURE when newPassword exceeds max length`() {
        val tooLongPassword = "a".repeat(101)
        client
            .graphqlMutation { changePasswordMutation("current-password", tooLongPassword) }
            .from(preconditions.fry)
            .executeAndVerifyValidationError(
                violationPath = "newPassword",
                error = "SizeConstraintViolated",
                message = "size must be between 0 and 100",
                path = "changePassword",
                params = mapOf("min" to "0", "max" to "100")
            )
    }

    @Test
    fun `should change password with minimum valid length`() {
        whenever(passwordEncoder.matches("a", preconditions.fry.passwordHash)) doReturn true
        whenever(passwordEncoder.encode("b")) doReturn "new password hash"

        client
            .graphqlMutation { changePasswordMutation("a", "b") }
            .from(preconditions.fry)
            .executeAndVerifySuccessResponse(
                "changePassword" to buildJsonObject {
                    put("success", true)
                }
            )
    }

    @Test
    fun `should change password with exactly max length`() {
        val maxLengthPassword = "a".repeat(100)
        whenever(passwordEncoder.matches(maxLengthPassword, preconditions.fry.passwordHash)) doReturn true
        whenever(passwordEncoder.encode(maxLengthPassword)) doReturn "new password hash"

        client
            .graphqlMutation { changePasswordMutation(maxLengthPassword, maxLengthPassword) }
            .from(preconditions.fry)
            .executeAndVerifySuccessResponse(
                "changePassword" to buildJsonObject {
                    put("success", true)
                }
            )
    }

    private fun testSuccessPath(user: PlatformUser) {
        whenever(passwordEncoder.matches("current-password", user.passwordHash)) doReturn true
        whenever(passwordEncoder.encode("new-password")) doReturn "new password hash"

        client
            .graphqlMutation { changePasswordMutation("current-password", "new-password") }
            .from(user)
            .executeAndVerifySuccessResponse(
                "changePassword" to buildJsonObject {
                    put("success", true)
                }
            )

        aggregateTemplate.findAll<PlatformUser>()
            .filter { it.id == user.id }
            .shouldBeSingle()
            .passwordHash.shouldBeEqual("new password hash")
    }

    private fun MutationProjection.changePasswordMutation(
        currentPassword: String,
        newPassword: String
    ): MutationProjection = changePassword(currentPassword, newPassword) {
        success
    }
}
