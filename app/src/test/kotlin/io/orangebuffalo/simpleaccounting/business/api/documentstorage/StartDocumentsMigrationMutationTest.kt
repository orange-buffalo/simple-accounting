package io.orangebuffalo.simpleaccounting.business.api.documentstorage

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.documents.migration.DocumentsMigration
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.client.MutationProjection
import io.orangebuffalo.simpleaccounting.tests.infra.api.*
import io.orangebuffalo.simpleaccounting.tests.infra.ui.TestDocumentsStorage
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findAll
import io.orangebuffalo.simpleaccounting.tests.infra.utils.JsonValues
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("startDocumentsMigration mutation")
class StartDocumentsMigrationMutationTest(
    @Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    private val preconditions by lazyPreconditions {
        object {
            val farnsworth = farnsworth()
            val workspaceAccessToken = workspaceAccessToken(
                workspace = workspace(owner = fry()),
                validTill = MOCK_TIME.plusSeconds(10000),
            )
        }
    }

    @Nested
    @DisplayName("Authorization")
    inner class Authorization {
        @Test
        fun `should return NOT_AUTHORIZED error for anonymous requests`() {
            client
                .graphqlMutation { startDocumentsMigrationMutation() }
                .fromAnonymous()
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.StartDocumentsMigration)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for admin user`() {
            client
                .graphqlMutation { startDocumentsMigrationMutation() }
                .from(preconditions.farnsworth)
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.StartDocumentsMigration)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for workspace access token`() {
            client
                .graphqlMutation { startDocumentsMigrationMutation() }
                .usingSharedWorkspaceToken(preconditions.workspaceAccessToken.token)
                .executeAndVerifyNotAuthorized(path = DgsConstants.MUTATION.StartDocumentsMigration)
        }
    }

    @Nested
    @DisplayName("Business Flow")
    inner class BusinessFlow {
        @Test
        fun `should create migration with documents outside current upload storage`() {
            val testData = preconditions {
                object {
                    val fry = fry().copy(documentsStorage = TestDocumentsStorage.STORAGE_ID).save()
                    val googleDriveDocument = document(
                        workspace = workspace(owner = fry),
                        storageId = "google-drive",
                        createdAt = MOCK_TIME.plusSeconds(2),
                    )
                    val localFsDocument = document(
                        workspace = workspace(owner = fry),
                        storageId = "local-fs",
                        createdAt = MOCK_TIME.plusSeconds(3),
                    )

                    init {
                        document(
                            workspace = workspace(owner = fry),
                            storageId = TestDocumentsStorage.STORAGE_ID,
                            createdAt = MOCK_TIME.plusSeconds(1),
                        )
                        document(
                            workspace = workspace(owner = zoidberg()),
                            storageId = "google-drive",
                            createdAt = MOCK_TIME.plusSeconds(4),
                        )
                    }
                }
            }

            client
                .graphqlMutation { startDocumentsMigrationMutation() }
                .from(testData.fry)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.StartDocumentsMigration to buildJsonObject {
                        put("id", JsonValues.ANY_STRING)
                        put("userId", testData.fry.id!!)
                        put("documentsToMigrate", buildJsonArray {
                            listOf(testData.googleDriveDocument.id!!, testData.localFsDocument.id!!)
                                .sorted()
                                .forEach { add(it) }
                        })
                        put("migratedDocumentsCount", 0)
                    }
                )

            val migration = aggregateTemplate.findAll<DocumentsMigration>()
                .filter { it.userId == testData.fry.id }
                .shouldHaveSize(1)
                .single()

            migration.documentsToMigrate.map { it.documentId }.shouldContainExactlyInAnyOrder(
                testData.googleDriveDocument.id!!,
                testData.localFsDocument.id!!,
            )
            migration.migratedDocumentsCount.shouldBe(0)
        }

        @Test
        fun `should create migration for all user documents when upload storage is not configured`() {
            val testData = preconditions {
                object {
                    val fry = fry().copy(documentsStorage = null).save()
                    val googleDriveDocument = document(
                        workspace = workspace(owner = fry),
                        storageId = "google-drive",
                        createdAt = MOCK_TIME.plusSeconds(1),
                    )
                    val localFsDocument = document(
                        workspace = workspace(owner = fry),
                        storageId = "local-fs",
                        createdAt = MOCK_TIME.plusSeconds(2),
                    )
                }
            }

            client
                .graphqlMutation { startDocumentsMigrationMutation() }
                .from(testData.fry)
                .executeAndVerifySuccessResponse(
                    DgsConstants.MUTATION.StartDocumentsMigration to buildJsonObject {
                        put("id", JsonValues.ANY_STRING)
                        put("userId", testData.fry.id!!)
                        put("documentsToMigrate", buildJsonArray {
                            listOf(testData.googleDriveDocument.id!!, testData.localFsDocument.id!!)
                                .sorted()
                                .forEach { add(it) }
                        })
                        put("migratedDocumentsCount", 0)
                    }
                )
        }
    }

    private fun MutationProjection.startDocumentsMigrationMutation(): MutationProjection = startDocumentsMigration {
        id
        userId
        documentsToMigrate
        migratedDocumentsCount
    }
}
