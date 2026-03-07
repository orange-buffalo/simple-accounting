package io.orangebuffalo.simpleaccounting.business.api

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.documents.storage.gdrive.GoogleDriveStorageIntegration
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.client.QueryProjection
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphql
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.GoogleDriveApiMocks
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.GoogleOAuthMocks
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.ThirdPartyApisMocksContextInitializer
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.ThirdPartyApisMocksListener
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestExecutionListeners

@TestExecutionListeners(
    listeners = [ThirdPartyApisMocksListener::class],
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS,
)
@ContextConfiguration(initializers = [ThirdPartyApisMocksContextInitializer::class])
class GoogleDriveStorageIntegrationStatusQueryTest(
    @param:Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    private val preconditions by lazyPreconditions {
        object {
            val fry = platformUser(userName = "Fry").withWorkspace()
            val leela = platformUser(userName = "Leela").withWorkspace().also {
                save(
                    GoogleDriveStorageIntegration(
                        userId = it.id!!,
                        folderId = "leela-root-folder-id",
                    )
                )
            }
            val workspaceToken = workspaceAccessToken(
                workspace = workspace(owner = fry),
                validTill = MOCK_TIME.plusSeconds(10000),
            )
        }
    }

    @Nested
    @DisplayName("Authorization")
    inner class Authorization {
        @Test
        fun `should return error when accessed anonymously`() {
            client
                .graphql { googleDriveStorageIntegrationStatusQuery() }
                .fromAnonymous()
                .executeAndVerifyNotAuthorized(
                    path = DgsConstants.QUERY.GoogleDriveStorageIntegrationStatus,
                )
        }

        @Test
        fun `should prohibit access with workspace token`() {
            client
                .graphql { googleDriveStorageIntegrationStatusQuery() }
                .usingSharedWorkspaceToken(preconditions.workspaceToken.token)
                .executeAndVerifyNotAuthorized(
                    path = DgsConstants.QUERY.GoogleDriveStorageIntegrationStatus,
                )
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {
        @Test
        fun `should return authorization required when no OAuth token is persisted`() {
            client
                .graphql { googleDriveStorageIntegrationStatusQueryWithoutAuthUrl() }
                .from(preconditions.fry)
                .executeAndVerifySuccessResponse(
                    DgsConstants.QUERY.GoogleDriveStorageIntegrationStatus to buildJsonObject {
                        put("authorizationRequired", true)
                        put("folderId", JsonNull)
                        put("folderName", JsonNull)
                    }
                )
        }

        @Test
        fun `should return integration status when OAuth token is persisted and folder exists`() {
            val accessToken = GoogleOAuthMocks.token().persist(preconditions.leela)
            GoogleDriveApiMocks.mockFindFile(
                fileId = "leela-root-folder-id",
                fileName = "leela-root-folder-name",
                expectedAuthToken = accessToken,
            )

            client
                .graphql { googleDriveStorageIntegrationStatusQuery() }
                .from(preconditions.leela)
                .executeAndVerifySuccessResponse(
                    DgsConstants.QUERY.GoogleDriveStorageIntegrationStatus to buildJsonObject {
                        put("authorizationRequired", false)
                        put("authorizationUrl", JsonNull)
                        put("folderId", "leela-root-folder-id")
                        put("folderName", "leela-root-folder-name")
                    }
                )
        }
    }

    private fun QueryProjection.googleDriveStorageIntegrationStatusQuery(): QueryProjection =
        googleDriveStorageIntegrationStatus {
            authorizationRequired
            authorizationUrl
            folderId
            folderName
        }

    private fun QueryProjection.googleDriveStorageIntegrationStatusQueryWithoutAuthUrl(): QueryProjection =
        googleDriveStorageIntegrationStatus {
            authorizationRequired
            folderId
            folderName
        }
}

