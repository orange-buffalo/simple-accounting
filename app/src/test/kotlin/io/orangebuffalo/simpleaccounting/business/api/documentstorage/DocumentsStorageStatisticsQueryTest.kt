package io.orangebuffalo.simpleaccounting.business.api.documentstorage

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.client.QueryProjection
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphql
import io.orangebuffalo.simpleaccounting.tests.infra.ui.TestDocumentsStorage
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class DocumentsStorageStatisticsQueryTest(
    @Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    private val preconditions by lazyPreconditions {
        object {
            val fry = platformUser(userName = "Fry")
            val fryWorkspace = workspace(owner = fry)
            val fryWorkspaceToken = workspaceAccessToken(
                workspace = fryWorkspace,
                validTill = MOCK_TIME.plusSeconds(10000),
            )
            val farnsworth = platformUser(userName = "Farnsworth", isAdmin = true)
        }
    }

    @Nested
    @DisplayName("Authorization")
    inner class Authorization {
        @Test
        fun `should return error when accessed anonymously`() {
            client
                .graphql { documentsStorageStatisticsQuery() }
                .fromAnonymous()
                .executeAndVerifyNotAuthorized(
                    path = DgsConstants.QUERY.DocumentsStorageStatistics,
                )
        }

        @Test
        fun `should prohibit access with workspace token`() {
            client
                .graphql { documentsStorageStatisticsQuery() }
                .usingSharedWorkspaceToken(preconditions.fryWorkspaceToken.token)
                .executeAndVerifyNotAuthorized(
                    path = DgsConstants.QUERY.DocumentsStorageStatistics,
                )
        }

        @Test
        fun `should allow access for regular user`() {
            client
                .graphql { documentsStorageStatisticsQuery() }
                .from(preconditions.fry)
                .executeAndVerifyResponse(
                    DgsConstants.QUERY.DocumentsStorageStatistics to emptyStatistics()
                )
        }

        @Test
        fun `should allow access for admin user`() {
            client
                .graphql { documentsStorageStatisticsQuery() }
                .from(preconditions.farnsworth)
                .executeAndVerifyResponse(
                    DgsConstants.QUERY.DocumentsStorageStatistics to emptyStatistics()
                )
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {
        @Test
        fun `should return empty list when user has no documents`() {
            client
                .graphql { documentsStorageStatisticsQuery() }
                .from(preconditions.fry)
                .executeAndVerifyResponse(
                    DgsConstants.QUERY.DocumentsStorageStatistics to emptyStatistics()
                )
        }

        @Test
        fun `should return statistics for documents in a single workspace`() {
            preconditions {
                document(workspace = preconditions.fryWorkspace, storageId = TestDocumentsStorage.STORAGE_ID)
                document(workspace = preconditions.fryWorkspace, storageId = TestDocumentsStorage.STORAGE_ID)
                document(workspace = preconditions.fryWorkspace, storageId = "noop")
            }

            client
                .graphql { documentsStorageStatisticsQuery() }
                .from(preconditions.fry)
                .executeAndVerifyResponse(
                    DgsConstants.QUERY.DocumentsStorageStatistics to buildJsonArray {
                        add(buildJsonObject {
                            put("storageId", "noop")
                            put("documentsCount", 1)
                        })
                        add(buildJsonObject {
                            put("storageId", TestDocumentsStorage.STORAGE_ID)
                            put("documentsCount", 2)
                        })
                    }
                )
        }

        @Test
        fun `should aggregate documents across multiple workspaces of the same user`() {
            val secondWorkspace = preconditions { workspace(owner = preconditions.fry) }
            preconditions {
                document(workspace = preconditions.fryWorkspace, storageId = TestDocumentsStorage.STORAGE_ID)
                document(workspace = secondWorkspace, storageId = TestDocumentsStorage.STORAGE_ID)
                document(workspace = secondWorkspace, storageId = "noop")
            }

            client
                .graphql { documentsStorageStatisticsQuery() }
                .from(preconditions.fry)
                .executeAndVerifyResponse(
                    DgsConstants.QUERY.DocumentsStorageStatistics to buildJsonArray {
                        add(buildJsonObject {
                            put("storageId", "noop")
                            put("documentsCount", 1)
                        })
                        add(buildJsonObject {
                            put("storageId", TestDocumentsStorage.STORAGE_ID)
                            put("documentsCount", 2)
                        })
                    }
                )
        }

        @Test
        fun `should not include documents from other users' workspaces`() {
            val zoidberg = preconditions { platformUser(userName = "Zoidberg") }
            val zoidbergWorkspace = preconditions { workspace(owner = zoidberg) }
            preconditions {
                document(workspace = zoidbergWorkspace, storageId = TestDocumentsStorage.STORAGE_ID)
            }

            client
                .graphql { documentsStorageStatisticsQuery() }
                .from(preconditions.fry)
                .executeAndVerifyResponse(
                    DgsConstants.QUERY.DocumentsStorageStatistics to emptyStatistics()
                )
        }

        @Test
        fun `should only include storages that have at least one document`() {
            preconditions {
                document(workspace = preconditions.fryWorkspace, storageId = "noop")
            }

            client
                .graphql { documentsStorageStatisticsQuery() }
                .from(preconditions.fry)
                .executeAndVerifyResponse(
                    DgsConstants.QUERY.DocumentsStorageStatistics to buildJsonArray {
                        add(buildJsonObject {
                            put("storageId", "noop")
                            put("documentsCount", 1)
                        })
                    }
                )
        }
    }

    private fun emptyStatistics(): JsonElement = buildJsonArray {}

    private fun QueryProjection.documentsStorageStatisticsQuery(): QueryProjection =
        documentsStorageStatistics {
            storageId
            documentsCount
        }
}
