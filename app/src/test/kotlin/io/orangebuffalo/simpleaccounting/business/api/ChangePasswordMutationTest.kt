package io.orangebuffalo.simpleaccounting.business.api

import io.kotest.matchers.equals.shouldBeEqual
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.client.MutationProjection
import io.orangebuffalo.simpleaccounting.tests.infra.api.*
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findAll
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeSingle
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
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

    @Nested
    @DisplayName("Authorization")
    inner class Authorization {
        @Test
        fun `should return NOT_AUTHORIZED error for anonymous requests`() {
            client
                .graphqlMutation { changePasswordMutation("current-password", "new-password") }
                .fromAnonymous()
                .executeAndVerifyNotAuthorized(
                    path = DgsConstants.MUTATION.ChangePassword
                )
        }

        @Test
        fun `should return NOT_AUTHORIZED error when changing password by a transient user`() {
            client
                .graphqlMutation { changePasswordMutation("current-password", "new-password") }
                .usingSharedWorkspaceToken(preconditions.workspaceAccessToken.token)
                .executeAndVerifyNotAuthorized(
                    path = DgsConstants.MUTATION.ChangePassword
                )
        }
    }

    @Nested
    @DisplayName("Inputs Validation")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class InputsValidation {
        fun testCases() = listOf(
            mustNotBeBlankTestCases("currentPassword") { value ->
                { changePasswordMutation(value, "new-password") }
            },
            mustNotBeBlankTestCases("newPassword") { value ->
                { changePasswordMutation("current-password", value) }
            },
            sizeConstraintTestCases("currentPassword", maxLength = 100) { value ->
                { changePasswordMutation(value, "new-password") }
            },
            sizeConstraintTestCases("newPassword", maxLength = 100) { value ->
                { changePasswordMutation("current-password", value) }
            },
        ).flatten()

        @ParameterizedTest(name = "{0}")
        @MethodSource("testCases")
        fun `should validate inputs`(testCase: GraphqlMutationInputTestCase) {
            client
                .buildInputValidationRequest(testCase)
                .from(preconditions.fry)
                .executeAndVerifyInputValidation(testCase, DgsConstants.MUTATION.ChangePassword)
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {
        @Test
        fun `should change password for regular user`() {
            testSuccessPath(preconditions.fry)
        }

        @Test
        fun `should change password for admin user`() {
            testSuccessPath(preconditions.farnsworth)
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
                    path = DgsConstants.MUTATION.ChangePassword
                )
        }

        private fun testSuccessPath(user: PlatformUser) {
            whenever(passwordEncoder.matches("current-password", user.passwordHash)) doReturn true
            whenever(passwordEncoder.encode("new-password")) doReturn "new password hash"

            client
                .graphqlMutation { changePasswordMutation("current-password", "new-password") }
                .from(user)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.ChangePassword to buildJsonObject {
                        put("success", true)
                    }
                )

            aggregateTemplate.findAll<PlatformUser>()
                .filter { it.id == user.id }
                .shouldBeSingle()
                .passwordHash.shouldBeEqual("new password hash")
        }
    }

    private fun MutationProjection.changePasswordMutation(
        currentPassword: String,
        newPassword: String
    ): MutationProjection = changePassword(currentPassword, newPassword) {
        success
    }
}
