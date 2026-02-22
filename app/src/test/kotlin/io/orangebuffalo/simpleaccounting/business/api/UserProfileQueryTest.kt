package io.orangebuffalo.simpleaccounting.business.api

import io.orangebuffalo.simpleaccounting.business.users.I18nSettings
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.client.QueryProjection
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphql
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class UserProfileQueryTest(
    @param:Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {
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
            val zoidberg = platformUser(
                userName = "Zoidberg",
                i18nSettings = I18nSettings(locale = "en_US", language = "en")
            )
            val farnsworth = platformUser(
                userName = "Farnsworth",
                isAdmin = true,
                documentsStorage = "local",
                i18nSettings = I18nSettings(locale = "fr_FR", language = "fr")
            )
            val fryWorkspace = workspace(owner = fry)
            val workspaceToken = workspaceAccessToken(
                workspace = fryWorkspace,
                validTill = MOCK_TIME.plusSeconds(10000),
            )
        }
    }

    @Test
    fun `should return error when accessed anonymously`() {
        client
            .graphql { fullUserProfile() }
            .fromAnonymous()
            .executeAndVerifyNotAuthorized(
                path = DgsConstants.QUERY.UserProfile,
            )
    }

    @Test
    fun `should return user profile for regular user (full data)`() {
        client
            .graphql { fullUserProfile() }
            .from(preconditions.fry)
            .executeAndVerifySuccessResponse(
                DgsConstants.QUERY.UserProfile to buildJsonObject {
                    put("userName", "Fry")
                    put("i18n", buildJsonObject {
                        put("locale", "en_AU")
                        put("language", "en")
                    })
                    put("documentsStorage", "google-drive")
                }
            )
    }

    @Test
    fun `should return user profile for regular user (minimal data)`() {
        client
            .graphql { fullUserProfile() }
            .from(preconditions.zoidberg)
            .executeAndVerifySuccessResponse(
                DgsConstants.QUERY.UserProfile to buildJsonObject {
                    put("userName", "Zoidberg")
                    put("i18n", buildJsonObject {
                        put("locale", "en_US")
                        put("language", "en")
                    })
                    put("documentsStorage", JsonNull)
                }
            )
    }

    @Test
    fun `should return user profile for admin user`() {
        client
            .graphql { fullUserProfile() }
            .from(preconditions.farnsworth)
            .executeAndVerifySuccessResponse(
                DgsConstants.QUERY.UserProfile to buildJsonObject {
                    put("userName", "Farnsworth")
                    put("i18n", buildJsonObject {
                        put("locale", "fr_FR")
                        put("language", "fr")
                    })
                    put("documentsStorage", "local")
                }
            )
    }

    @Test
    fun `should prohibit access with workspace token`() {
        client
            .graphql { fullUserProfile() }
            .usingSharedWorkspaceToken(preconditions.workspaceToken.token)
            .executeAndVerifyNotAuthorized(
                path = DgsConstants.QUERY.UserProfile,
            )
    }

    private fun QueryProjection.fullUserProfile(): QueryProjection = userProfile {
        userName
        i18n {
            locale
            language
        }
        documentsStorage
    }
}
