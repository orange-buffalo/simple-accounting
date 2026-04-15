package io.orangebuffalo.simpleaccounting.business.api.users

import io.kotest.matchers.collections.shouldNotContain
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.users.UserActivationToken
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.client.MutationProjection
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphqlMutation
import io.orangebuffalo.simpleaccounting.tests.infra.utils.JsonValues
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeSingle
import io.orangebuffalo.simpleaccounting.tests.infra.utils.withHint
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("createUserActivationToken mutation")
class CreateUserActivationTokenMutationTest(
    @Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry()
            val farnsworth = farnsworth()
            val userWithoutToken = platformUser(activated = false)
            val userWithToken = platformUser(activated = false)
            val existingToken = userActivationToken(
                user = userWithToken,
                token = "existing-token",
                expiresAt = MOCK_TIME.plusSeconds(1),
            )
            val activatedUser = platformUser(activated = true)
        }
    }

    @Nested
    @DisplayName("Authorization")
    inner class Authorization {
        @Test
        fun `should return NOT_AUTHORIZED error for anonymous requests`() {
            client.graphqlMutation { createUserActivationTokenMutation(userId = 42) }
                .fromAnonymous()
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.CreateUserActivationToken)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for regular user`() {
            client.graphqlMutation { createUserActivationTokenMutation(userId = 42) }
                .from(preconditions.fry)
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.CreateUserActivationToken)
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {
        @Test
        fun `should return ENTITY_NOT_FOUND for non-existing user`() {
            client.graphqlMutation { createUserActivationTokenMutation(userId = 100500) }
                .from(preconditions.farnsworth)
                .executeAndVerifyEntityNotFoundError(path = DgsConstants.MUTATION.CreateUserActivationToken)
        }

        @Test
        fun `should return USER_ALREADY_ACTIVATED error for activated user`() {
            client.graphqlMutation {
                createUserActivationTokenMutation(userId = preconditions.activatedUser.id!!)
            }
                .from(preconditions.farnsworth)
                .executeAndVerifyBusinessError(
                    message = "User ${preconditions.activatedUser.id} is already activated",
                    errorCode = "USER_ALREADY_ACTIVATED",
                    path = DgsConstants.MUTATION.CreateUserActivationToken,
                )
        }

        @Test
        fun `should create token for user without existing token`() {
            client.graphqlMutation {
                createUserActivationTokenMutation(userId = preconditions.userWithoutToken.id!!)
            }
                .from(preconditions.farnsworth)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.CreateUserActivationToken to buildJsonObject {
                        put("token", JsonValues.ANY_STRING)
                        put("expiresAt", "1999-03-29T04:01:02.042Z")
                    }
                )

            withHint("Token should be created in database") {
                aggregateTemplate.findAll(UserActivationToken::class.java)
                    .filter { it.userId == preconditions.userWithoutToken.id }
                    .shouldBeSingle()
            }
        }

        @Test
        fun `should replace existing token when creating new one`() {
            client.graphqlMutation {
                createUserActivationTokenMutation(userId = preconditions.userWithToken.id!!)
            }
                .from(preconditions.farnsworth)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.CreateUserActivationToken to buildJsonObject {
                        put("token", JsonValues.ANY_STRING)
                        put("expiresAt", "1999-03-29T04:01:02.042Z")
                    }
                )

            withHint("Existing token should be removed") {
                aggregateTemplate.findAll(UserActivationToken::class.java)
                    .shouldNotContain(preconditions.existingToken)
            }

            withHint("New token should be created in database") {
                aggregateTemplate.findAll(UserActivationToken::class.java)
                    .filter { it.userId == preconditions.userWithToken.id }
                    .shouldBeSingle()
            }
        }
    }

    private fun MutationProjection.createUserActivationTokenMutation(userId: Long): MutationProjection =
        createUserActivationToken(userId = userId) {
            token
            expiresAt
        }
}
