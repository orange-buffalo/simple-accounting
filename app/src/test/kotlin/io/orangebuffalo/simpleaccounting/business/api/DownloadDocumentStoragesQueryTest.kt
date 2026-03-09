package io.orangebuffalo.simpleaccounting.business.api

import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.client.QueryProjection
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.api.expectThatJsonBody
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphql
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

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
                .execute()
                .expectStatus().isOk
                .expectThatJsonBody {
                    extractStorageIds(this).shouldNotBeNull()
                }
        }

        @Test
        fun `should allow access for admin user`() {
            client
                .graphql { downloadDocumentStoragesQuery() }
                .from(preconditions.farnsworth)
                .execute()
                .expectStatus().isOk
                .expectThatJsonBody {
                    extractStorageIds(this).shouldNotBeNull()
                }
        }

        @Test
        fun `should allow access with workspace token`() {
            client
                .graphql { downloadDocumentStoragesQuery() }
                .usingSharedWorkspaceToken(preconditions.fryWorkspaceToken.token)
                .execute()
                .expectStatus().isOk
                .expectThatJsonBody {
                    extractStorageIds(this).shouldNotBeNull()
                }
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
                .execute()
                .expectStatus().isOk
                .expectThatJsonBody {
                    val storageIds = extractStorageIds(this)
                    storageIds.shouldContainAll("local-fs", "noop")
                }
        }

        @Test
        fun `should return storages available for download for admin user`() {
            client
                .graphql { downloadDocumentStoragesQuery() }
                .from(preconditions.farnsworth)
                .execute()
                .expectStatus().isOk
                .expectThatJsonBody {
                    val storageIds = extractStorageIds(this)
                    storageIds.shouldContainAll("local-fs", "noop")
                }
        }

        @Test
        fun `should return storages available for download via workspace token`() {
            client
                .graphql { downloadDocumentStoragesQuery() }
                .usingSharedWorkspaceToken(preconditions.fryWorkspaceToken.token)
                .execute()
                .expectStatus().isOk
                .expectThatJsonBody {
                    val storageIds = extractStorageIds(this)
                    storageIds.shouldContainAll("local-fs", "noop")
                }
        }

        @Test
        fun `should not include Google Drive when user has no integration`() {
            client
                .graphql { downloadDocumentStoragesQuery() }
                .from(preconditions.fry)
                .execute()
                .expectStatus().isOk
                .expectThatJsonBody {
                    val storageIds = extractStorageIds(this)
                    storageIds.shouldNotContain("google-drive")
                }
        }

        @Test
        fun `should not include Google Drive for workspace token when owner has no integration`() {
            client
                .graphql { downloadDocumentStoragesQuery() }
                .usingSharedWorkspaceToken(preconditions.fryWorkspaceToken.token)
                .execute()
                .expectStatus().isOk
                .expectThatJsonBody {
                    val storageIds = extractStorageIds(this)
                    storageIds.shouldNotContain("google-drive")
                }
        }

        @Test
        fun `should resolve storages based on workspace owner for workspace token`() {
            client
                .graphql { downloadDocumentStoragesQuery() }
                .usingSharedWorkspaceToken(preconditions.zoidbergWorkspaceToken.token)
                .execute()
                .expectStatus().isOk
                .expectThatJsonBody {
                    val storageIds = extractStorageIds(this)
                    storageIds.shouldContainAll("local-fs", "noop")
                    storageIds.shouldNotContain("google-drive")
                }
        }
    }

    private fun extractStorageIds(responseBody: String): Set<String> {
        val json = Json.parseToJsonElement(responseBody).jsonObject
        val data = json["data"]?.jsonObject.shouldNotBeNull()
        val storages = data["getDownloadDocumentStorages"]?.jsonArray.shouldNotBeNull()
        return storages.map { it.jsonObject["id"]!!.jsonPrimitive.content }.toSet()
    }

    private fun QueryProjection.downloadDocumentStoragesQuery(): QueryProjection =
        getDownloadDocumentStorages {
            id
        }
}
