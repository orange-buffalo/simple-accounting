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
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonArray
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
class DownloadDocumentStoragesQueryTest(
    @param:Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    private val preconditions by lazyPreconditions {
        object {
            val fry = platformUser(
                userName = "Fry",
                documentsStorage = "noop",
            )
            val farnsworth = platformUser(
                userName = "Farnsworth",
                isAdmin = true,
                documentsStorage = "noop",
            )
            val fryWorkspace = workspace(owner = fry)
            val fryWorkspaceToken = workspaceAccessToken(
                workspace = fryWorkspace,
                validTill = MOCK_TIME.plusSeconds(10000),
            )
            val zoidbergWorkspaceToken = platformUser(
                userName = "Zoidberg",
                documentsStorage = "noop",
            ).let {
                val zoidbergWorkspace = workspace(owner = it)
                workspaceAccessToken(
                    workspace = zoidbergWorkspace,
                    token = "zoidbergToken",
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
                .graphql { downloadDocumentStoragesQuery() }
                .fromAnonymous()
                .executeAndVerifyNotAuthorized(
                    path = DgsConstants.QUERY.GetDownloadDocumentStorages,
                )
        }

        @Test
        fun `should allow access for regular user`() {
            client
                .graphql { downloadDocumentStoragesQuery() }
                .from(preconditions.fry)
                .executeAndVerifyResponse(
                    DgsConstants.QUERY.GetDownloadDocumentStorages to storagesWithoutGoogleDrive()
                )
        }

        @Test
        fun `should allow access for admin user`() {
            client
                .graphql { downloadDocumentStoragesQuery() }
                .from(preconditions.farnsworth)
                .executeAndVerifyResponse(
                    DgsConstants.QUERY.GetDownloadDocumentStorages to storagesWithoutGoogleDrive()
                )
        }

        @Test
        fun `should allow access with workspace token`() {
            client
                .graphql { downloadDocumentStoragesQuery() }
                .usingSharedWorkspaceToken(preconditions.fryWorkspaceToken.token)
                .executeAndVerifyResponse(
                    DgsConstants.QUERY.GetDownloadDocumentStorages to storagesWithoutGoogleDrive()
                )
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {
        @Test
        fun `should return storages available for download for regular user`() {
            client
                .graphql { downloadDocumentStoragesQuery() }
                .from(preconditions.fry)
                .executeAndVerifyResponse(
                    DgsConstants.QUERY.GetDownloadDocumentStorages to storagesWithoutGoogleDrive()
                )
        }

        @Test
        fun `should return storages available for download via workspace token`() {
            client
                .graphql { downloadDocumentStoragesQuery() }
                .usingSharedWorkspaceToken(preconditions.fryWorkspaceToken.token)
                .executeAndVerifyResponse(
                    DgsConstants.QUERY.GetDownloadDocumentStorages to storagesWithoutGoogleDrive()
                )
        }

        @Test
        fun `should resolve storages based on workspace owner for workspace token`() {
            client
                .graphql { downloadDocumentStoragesQuery() }
                .usingSharedWorkspaceToken(preconditions.zoidbergWorkspaceToken.token)
                .executeAndVerifyResponse(
                    DgsConstants.QUERY.GetDownloadDocumentStorages to storagesWithoutGoogleDrive()
                )
        }
    }

    @Nested
    @DisplayName("Google Drive Storage Availability")
    inner class GoogleDriveStorageAvailability {

        @Test
        fun `should include Google Drive when integration is configured and folder is accessible`() {
            val accessToken = GoogleOAuthMocks.token().persist(preconditions.fry)
            preconditions {
                save(
                    GoogleDriveStorageIntegration(
                        userId = preconditions.fry.id!!,
                        folderId = "fry-root-folder-id",
                    )
                )
            }
            GoogleDriveApiMocks.mockFindFile(
                fileId = "fry-root-folder-id",
                fileName = "simple-accounting",
                expectedAuthToken = accessToken,
            )

            client
                .graphql { downloadDocumentStoragesQuery() }
                .from(preconditions.fry)
                .executeAndVerifyResponse(
                    DgsConstants.QUERY.GetDownloadDocumentStorages to storagesWithGoogleDrive()
                )
        }

        @Test
        fun `should not include Google Drive when user has no integration record`() {
            client
                .graphql { downloadDocumentStoragesQuery() }
                .from(preconditions.fry)
                .executeAndVerifyResponse(
                    DgsConstants.QUERY.GetDownloadDocumentStorages to storagesWithoutGoogleDrive()
                )
        }

        @Test
        fun `should not include Google Drive when integration has no folder id`() {
            preconditions {
                save(
                    GoogleDriveStorageIntegration(
                        userId = preconditions.fry.id!!,
                        folderId = null,
                    )
                )
            }

            client
                .graphql { downloadDocumentStoragesQuery() }
                .from(preconditions.fry)
                .executeAndVerifyResponse(
                    DgsConstants.QUERY.GetDownloadDocumentStorages to storagesWithoutGoogleDrive()
                )
        }

        @Test
        fun `should not include Google Drive when OAuth authorization is missing`() {
            preconditions {
                save(
                    GoogleDriveStorageIntegration(
                        userId = preconditions.fry.id!!,
                        folderId = "fry-root-folder-id",
                    )
                )
            }

            client
                .graphql { downloadDocumentStoragesQuery() }
                .from(preconditions.fry)
                .executeAndVerifyResponse(
                    DgsConstants.QUERY.GetDownloadDocumentStorages to storagesWithoutGoogleDrive()
                )
        }
    }

    private fun storagesWithoutGoogleDrive(): JsonElement = buildJsonArray {
        add(buildJsonObject { put("id", "local-fs") })
        add(buildJsonObject { put("id", "noop") })
        add(buildJsonObject { put("id", "test-storage") })
    }

    private fun storagesWithGoogleDrive(): JsonElement = buildJsonArray {
        add(buildJsonObject { put("id", "google-drive") })
        add(buildJsonObject { put("id", "local-fs") })
        add(buildJsonObject { put("id", "noop") })
        add(buildJsonObject { put("id", "test-storage") })
    }

    private fun QueryProjection.downloadDocumentStoragesQuery(): QueryProjection =
        getDownloadDocumentStorages {
            id
        }
}
