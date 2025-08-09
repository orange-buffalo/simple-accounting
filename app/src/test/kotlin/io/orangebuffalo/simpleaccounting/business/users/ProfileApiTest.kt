package io.orangebuffalo.simpleaccounting.business.users

import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.tests.infra.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphql
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class ProfileApiTest(
    @param:Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    @Test
    fun `should return user profile`() {
        val preconditions by lazyPreconditions {
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

        client
            .graphql {
                myProfile {
                    userName
                    i18n {
                        locale
                        language
                    }
                }
            }
            .from(preconditions.fry)
            .executeAndVerifySuccessResponse(
                DgsConstants.QUERY.MyProfile to buildJsonObject {
                    put("userName", "Fry")
                    put("i18n", buildJsonObject {
                        put("locale", "en_AU")
                        put("language", "en")
                    })
                }
            )
    }
}
