package io.orangebuffalo.simpleaccounting.business.api.users

import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldNotContain
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import io.orangebuffalo.simpleaccounting.business.users.UserActivationToken
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.client.MutationProjection
import io.orangebuffalo.simpleaccounting.tests.infra.api.*
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.tests.infra.utils.withHint
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.DisplayName
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("activateUser mutation")
class ActivateUserMutationTest(
    @Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry()
            val user = platformUser(activated = false)
            val activeToken = userActivationToken(
                user = user,
                token = "active-token",
                expiresAt = MOCK_TIME.plusSeconds(1),
            )
            val expiredToken = userActivationToken(
                token = "expired-token",
                expiresAt = MOCK_TIME.minusSeconds(1),
            )
        }
    }

    @Nested
    @DisplayName("Authorization")
    inner class Authorization {
        @Test
        fun `should allow anonymous access`() {
            whenever(passwordEncoder.encode("qwerty123")) doReturn "encoded-password"

            client.graphqlMutation { activateUserMutation(token = preconditions.activeToken.token) }
                .fromAnonymous()
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.ActivateUser to buildJsonObject { put("success", true) }
                )
        }

        @Test
        fun `should allow regular user access`() {
            whenever(passwordEncoder.encode("qwerty123")) doReturn "encoded-password"

            client.graphqlMutation { activateUserMutation(token = preconditions.activeToken.token) }
                .from(preconditions.fry)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.ActivateUser to buildJsonObject { put("success", true) }
                )
        }
    }

    @Nested
    @DisplayName("Inputs Validation")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class InputsValidation {
        fun testCases() = listOf(
            mustNotBeBlankTestCases("password") { value ->
                activateUserMutation(password = value)
            },
            sizeConstraintTestCases("password", maxLength = 100) { value ->
                activateUserMutation(password = value)
            },
        ).flatten()

        @ParameterizedTest(name = "{0}")
        @MethodSource("testCases")
        fun `should validate inputs`(testCase: GraphqlMutationInputTestCase) {
            client
                .buildInputValidationRequest(testCase)
                .fromAnonymous()
                .executeAndVerifyInputValidation(testCase, DgsConstants.MUTATION.ActivateUser)
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {
        @Test
        fun `should return ENTITY_NOT_FOUND error for non-existing token`() {
            client.graphqlMutation { activateUserMutation(token = "non-existing-token") }
                .fromAnonymous()
                .executeAndVerifyEntityNotFoundError(path = DgsConstants.MUTATION.ActivateUser)
        }

        @Test
        fun `should return TOKEN_EXPIRED business error for expired token`() {
            client.graphqlMutation { activateUserMutation(token = preconditions.expiredToken.token) }
                .fromAnonymous()
                .executeAndVerifyBusinessError(
                    message = "Token expired",
                    errorCode = "TOKEN_EXPIRED",
                    path = DgsConstants.MUTATION.ActivateUser,
                )
        }

        @Test
        fun `should activate user account with valid token`() {
            whenever(passwordEncoder.encode("qwerty123")) doReturn "encoded-password"

            client.graphqlMutation { activateUserMutation(token = preconditions.activeToken.token) }
                .fromAnonymous()
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.ActivateUser to buildJsonObject { put("success", true) }
                )

            withHint("User should be activated") {
                val user = aggregateTemplate.findById(preconditions.user.id!!, PlatformUser::class.java)
                user.activated.shouldBeTrue()
            }

            withHint("Token should be removed") {
                aggregateTemplate.findAll(UserActivationToken::class.java)
                    .shouldNotContain(preconditions.activeToken)
            }
        }
    }

    private fun MutationProjection.activateUserMutation(
        token: String = preconditions.activeToken.token,
        password: String = "qwerty123",
    ): MutationProjection = activateUser(token = token, password = password) {
        success
    }
}
