package io.orangebuffalo.simpleaccounting.business.users

import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.tests.infra.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphql
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class UserProfileApiTest(
    @param:Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    @Nested
    inner class UserProfileQuery {
        private val preconditions by lazyPreconditions {
            object {
                val fry = platformUser(
                    userName = "Fry",
                    documentsStorage = "google-drive",
                    i18nSettings = I18nSettings(
                        locale = "en_AU",
                        language = "en",
                    )
                )
            }
        }

        @Test
        fun `should return user profile for regular user`() {
            client
                .graphql {
                    userProfile {
                        userName
                        i18n {
                            locale
                            language
                        }
                    }
                }
                .from(preconditions.fry)
                .executeAndVerifySuccessResponse(
                    DgsConstants.QUERY.UserProfile to buildJsonObject {
                        put("userName", "Fry")
                        put("i18n", buildJsonObject {
                            put("locale", "en_AU")
                            put("language", "en")
                        })
                    }
                )
        }

        @Test
        fun `should return error when accessed anonymously`() {
            client
                .graphql {
                    userProfile {
                        userName
                    }
                }
                .fromAnonymous()
                .executeAndVerifySingleError(
                    message = "User is not authenticated",
                    errorType = "NOT_AUTHORIZED",
                    locationLine = 2,
                    locationColumn = 3,
                    path = DgsConstants.QUERY.UserProfile,
                )
        }
    }
}
