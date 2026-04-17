package io.orangebuffalo.simpleaccounting.business.api.users

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphql
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("user query")
class UserQueryTest(
    @Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    private val preconditions by lazyPreconditions {
        object {
            val farnsworth = farnsworth()
            val fry = fry()
        }
    }

    @Nested
    @DisplayName("Authorization")
    inner class Authorization {
        @Test
        fun `should return NOT_AUTHORIZED error for anonymous requests`() {
            client.graphql {
                userQuery(id = preconditions.fry.id!!)
            }
                .fromAnonymous()
                .executeAndVerifyNotAuthorized(path = DgsConstants.QUERY.User)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for regular user`() {
            client.graphql {
                userQuery(id = preconditions.fry.id!!)
            }
                .from(preconditions.fry)
                .executeAndVerifyNotAuthorized(path = DgsConstants.QUERY.User)
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {
        @Test
        fun `should return user by id`() {
            client.graphql {
                userQuery(id = preconditions.fry.id!!)
            }
                .from(preconditions.farnsworth)
                .executeAndVerifyResponse(
                    "user" to buildJsonObject {
                        put("id", preconditions.fry.id!!)
                        put("userName", "Fry")
                        put("admin", false)
                        put("activated", true)
                    }
                )
        }

        @Test
        fun `should return entity not found error for non-existent user`() {
            client.graphql {
                userQuery(id = Long.MAX_VALUE)
            }
                .from(preconditions.farnsworth)
                .executeAndVerifyEntityNotFoundError(path = DgsConstants.QUERY.User)
        }
    }

    private fun io.orangebuffalo.simpleaccounting.infra.graphql.client.QueryProjection.userQuery(id: Long) =
        user(id = id) {
            this.id
            this.userName
            this.admin
            this.activated
        }
}
