package io.orangebuffalo.simpleaccounting.business.api.documentstorage

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.encodeCursor
import io.orangebuffalo.simpleaccounting.tests.infra.api.*
import io.orangebuffalo.simpleaccounting.tests.infra.database.EntitiesFactory
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.JsonArrayBuilder
import kotlinx.serialization.json.putJsonArray
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@DisplayName("documentsMigrations query")
class DocumentsMigrationsQueryTest(
    @Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry()
            val farnsworth = farnsworth()
            val workspaceAccessToken = workspaceAccessToken(
                workspace = workspace(owner = fry),
                validTill = MOCK_TIME.plusSeconds(10000),
            )
        }
    }

    @Nested
    @DisplayName("Authorization")
    inner class Authorization {
        @Test
        fun `should return NOT_AUTHORIZED error for anonymous requests`() {
            client.graphql {
                documentsMigrations(first = 10) { totalCount }
            }
                .fromAnonymous()
                .executeAndVerifyNotAuthorized(path = DgsConstants.QUERY.DocumentsMigrations)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for admin user`() {
            client.graphql {
                documentsMigrations(first = 10) { totalCount }
            }
                .from(preconditions.farnsworth)
                .executeAndVerifyNotAuthorized(path = DgsConstants.QUERY.DocumentsMigrations)
        }

        @Test
        fun `should return NOT_AUTHORIZED error for workspace access token`() {
            client.graphql {
                documentsMigrations(first = 10) { totalCount }
            }
                .usingSharedWorkspaceToken(preconditions.workspaceAccessToken.token)
                .executeAndVerifyNotAuthorized(path = DgsConstants.QUERY.DocumentsMigrations)
        }
    }

    @Nested
    @DisplayName("Pagination")
    inner class Pagination {

        private fun EntitiesFactory.threeMigrations() = object {
            val fry = fry()
            private val workspace = workspace(owner = fry)
            private val slurmReceipt = document(workspace = workspace, name = "Slurm receipt")
            private val robotOilReceipt = document(workspace = workspace, name = "Robot oil receipt")
            private val spaceshipPartsReceipt = document(workspace = workspace, name = "Spaceship parts receipt")

            val lunaParkMigration = documentsMigration(
                user = fry,
                documentsToMigrate = setOf(slurmReceipt),
                createdAt = MOCK_TIME.plusSeconds(100),
            )
            val omicronMigration = documentsMigration(
                user = fry,
                documentsToMigrate = setOf(robotOilReceipt),
                migratedDocumentsCount = 1,
                completedAt = MOCK_TIME.plusSeconds(500),
                createdAt = MOCK_TIME.plusSeconds(200),
            )
            val marsMigration = documentsMigration(
                user = fry,
                documentsToMigrate = setOf(spaceshipPartsReceipt),
                createdAt = MOCK_TIME.plusSeconds(300),
            )
        }

        @Test
        fun `should return first page sorted by createdAt descending with pageInfo`() {
            val testData = preconditions { threeMigrations() }

            client.graphql {
                documentsMigrations(first = 2) {
                    edges {
                        cursor
                        node {
                            id
                            requestedDocumentsCount
                            migratedDocumentsCount
                            completedAt
                            documentsToMigrate { name }
                        }
                    }
                    pageInfo {
                        startCursor
                        endCursor
                        hasPreviousPage
                        hasNextPage
                    }
                    totalCount
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "documentsMigrations" to buildJsonObject {
                        putJsonArray("edges") {
                            add(documentsMigrationEdge(
                                cursor = encodeCursor(testData.marsMigration.createdAt!!),
                                id = testData.marsMigration.id!!,
                                documentName = "Spaceship parts receipt",
                                requestedDocumentsCount = 1,
                                migratedDocumentsCount = 0,
                                completedAt = JsonNull,
                            ))
                            add(documentsMigrationEdge(
                                cursor = encodeCursor(testData.omicronMigration.createdAt!!),
                                id = testData.omicronMigration.id!!,
                                documentName = "Robot oil receipt",
                                requestedDocumentsCount = 1,
                                migratedDocumentsCount = 1,
                                completedAt = testData.omicronMigration.completedAt!!.toString(),
                            ))
                        }
                        put("pageInfo", buildJsonObject {
                            put("startCursor", encodeCursor(testData.marsMigration.createdAt!!))
                            put("endCursor", encodeCursor(testData.omicronMigration.createdAt!!))
                            put("hasPreviousPage", false)
                            put("hasNextPage", true)
                        })
                        put("totalCount", 3)
                    }
                )
        }

        @Test
        fun `should return second page using after cursor`() {
            val testData = preconditions { threeMigrations() }

            client.graphql {
                documentsMigrations(first = 10, after = encodeCursor(testData.omicronMigration.createdAt!!)) {
                    edges {
                        cursor
                        node { id }
                    }
                    pageInfo {
                        startCursor
                        endCursor
                        hasPreviousPage
                        hasNextPage
                    }
                    totalCount
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "documentsMigrations" to buildJsonObject {
                        putJsonArray("edges") {
                            add(buildJsonObject {
                                put("cursor", encodeCursor(testData.lunaParkMigration.createdAt!!))
                                put("node", buildJsonObject { put("id", testData.lunaParkMigration.id!!) })
                            })
                        }
                        put("pageInfo", buildJsonObject {
                            put("startCursor", encodeCursor(testData.lunaParkMigration.createdAt!!))
                            put("endCursor", encodeCursor(testData.lunaParkMigration.createdAt!!))
                            put("hasPreviousPage", true)
                            put("hasNextPage", false)
                        })
                        put("totalCount", 3)
                    }
                )
        }

        @Test
        fun `should not include other users migrations`() {
            val testData = preconditions {
                object {
                    val fry = fry()
                    val fryMigration = documentsMigration(user = fry, createdAt = MOCK_TIME.plusSeconds(100))
                }.also {
                    documentsMigration(user = zoidberg(), createdAt = MOCK_TIME.plusSeconds(200))
                }
            }

            client.graphql {
                documentsMigrations(first = 10) {
                    edges { node { id } }
                    totalCount
                }
            }
                .from(testData.fry)
                .executeAndVerifyResponse(
                    "documentsMigrations" to documentsMigrationsConnection(totalCount = 1) {
                        add(buildJsonObject {
                            put("node", buildJsonObject { put("id", testData.fryMigration.id!!) })
                        })
                    }
                )
        }

        @Test
        fun `should reject first greater than 500`() {
            client.graphql {
                documentsMigrations(first = 501) { totalCount }
            }
                .from(preconditions.fry)
                .executeAndVerifyValidationError(
                    violationPath = "first",
                    error = "MaxConstraintViolated",
                    message = "must be less than or equal to 500",
                    path = DgsConstants.QUERY.DocumentsMigrations,
                    params = mapOf("value" to "500"),
                )
        }

        @Test
        fun `should reject first less than 1`() {
            client.graphql {
                documentsMigrations(first = 0) { totalCount }
            }
                .from(preconditions.fry)
                .executeAndVerifyValidationError(
                    violationPath = "first",
                    error = "MinConstraintViolated",
                    message = "must be greater than or equal to 1",
                    path = DgsConstants.QUERY.DocumentsMigrations,
                    params = mapOf("value" to "1"),
                )
        }
    }
}

private fun documentsMigrationEdge(
    cursor: String,
    id: String,
    documentName: String,
    requestedDocumentsCount: Int,
    migratedDocumentsCount: Int,
    completedAt: JsonElement,
): JsonElement = buildJsonObject {
    put("cursor", cursor)
    put("node", buildJsonObject {
        put("id", id)
        put("requestedDocumentsCount", requestedDocumentsCount)
        put("migratedDocumentsCount", migratedDocumentsCount)
        put("completedAt", completedAt)
        putJsonArray("documentsToMigrate") {
            add(buildJsonObject { put("name", documentName) })
        }
    })
}

private fun documentsMigrationEdge(
    cursor: String,
    id: String,
    documentName: String,
    requestedDocumentsCount: Int,
    migratedDocumentsCount: Int,
    completedAt: String,
): JsonElement = documentsMigrationEdge(
    cursor = cursor,
    id = id,
    documentName = documentName,
    requestedDocumentsCount = requestedDocumentsCount,
    migratedDocumentsCount = migratedDocumentsCount,
    completedAt = JsonPrimitive(completedAt),
)

private fun documentsMigrationsConnection(
    totalCount: Int,
    edgesSpec: JsonArrayBuilder.() -> Unit,
): JsonElement = buildJsonObject {
    putJsonArray("edges") { edgesSpec() }
    put("totalCount", totalCount)
}
