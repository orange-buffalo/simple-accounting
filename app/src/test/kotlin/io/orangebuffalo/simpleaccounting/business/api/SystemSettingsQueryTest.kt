package io.orangebuffalo.simpleaccounting.business.api

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.client.QueryProjection
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphql
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class SystemSettingsQueryTest(
    @param:Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    private val preconditions by lazyPreconditions {
        object {
            val fry = platformUser(userName = "Fry")
            val workspaceToken = workspace(owner = fry).let { ws ->
                workspaceAccessToken(
                    workspace = ws,
                    validTill = MOCK_TIME.plusSeconds(10000),
                )
            }
        }
    }

    @Nested
    @DisplayName("Authorization")
    inner class Authorization {
        @Test
        fun `should return error when accessed anonymously`() {
            client
                .graphql { systemSettingsQuery() }
                .fromAnonymous()
                .executeAndVerifyNotAuthorized(
                    path = DgsConstants.QUERY.SystemSettings,
                )
        }

        @Test
        fun `should prohibit access with workspace token`() {
            client
                .graphql { systemSettingsQuery() }
                .usingSharedWorkspaceToken(preconditions.workspaceToken.token)
                .executeAndVerifyNotAuthorized(
                    path = DgsConstants.QUERY.SystemSettings,
                )
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {
        @Test
        fun `should return system settings`() {
            client
                .graphql { systemSettingsQuery() }
                .from(preconditions.fry)
                .executeAndVerifySuccessResponse(
                    DgsConstants.QUERY.SystemSettings to buildJsonObject {
                        put("localFileSystemDocumentsStorageEnabled", false)
                    }
                )
        }
    }

    private fun QueryProjection.systemSettingsQuery(): QueryProjection =
        systemSettings {
            localFileSystemDocumentsStorageEnabled
        }
}
