package io.orangebuffalo.simpleaccounting.business.api

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.infra.graphql.DgsConstants
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.encodeCursor
import io.orangebuffalo.simpleaccounting.tests.infra.api.ApiTestClient
import io.orangebuffalo.simpleaccounting.tests.infra.api.graphql
import io.orangebuffalo.simpleaccounting.tests.infra.database.EntitiesFactory
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class CustomersQueryTest(
    @Autowired private val client: ApiTestClient,
) : SaIntegrationTestBase() {

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry()
            val workspace = workspace(owner = fry)

            init {
                customer(workspace = workspace, name = "Slurm Corp")
                customer(workspace = workspace, name = "Mom's Friendly Robot Company")
            }

            val zoidberg = zoidberg().withWorkspace()
            val workspaceToken = workspaceAccessToken(
                workspace = workspace,
                validTill = MOCK_TIME.plusSeconds(10000),
            )
        }
    }

    @Nested
    @DisplayName("Top-level customers query")
    inner class TopLevelQuery {

        @Nested
        @DisplayName("Authorization")
        inner class Authorization {
            @Test
            fun `should return error when accessed anonymously`() {
                client.graphql {
                    customers(workspaceId = preconditions.workspace.id!!.toInt(), first = 10) {
                        edges {
                            node { name }
                        }
                    }
                }
                    .fromAnonymous()
                    .executeAndVerifyNotAuthorized(
                        path = DgsConstants.QUERY.Customers,
                    )
            }

            @Test
            fun `should prohibit access with workspace token`() {
                client.graphql {
                    customers(workspaceId = preconditions.workspace.id!!.toInt(), first = 10) {
                        edges {
                            node { name }
                        }
                    }
                }
                    .usingSharedWorkspaceToken(preconditions.workspaceToken.token)
                    .executeAndVerifyNotAuthorized(
                        path = DgsConstants.QUERY.Customers,
                    )
            }
        }

        @Nested
        @DisplayName("Business Flow")
        inner class BusinessFlow {
            @Test
            fun `should return customers for workspace`() {
                client.graphql {
                    customers(workspaceId = preconditions.workspace.id!!.toInt(), first = 10) {
                        edges {
                            node { name }
                        }
                        totalCount
                    }
                }
                    .from(preconditions.fry)
                    .executeAndVerifyResponse(
                        "customers" to customersConnection(totalCount = 2) {
                            customerEdge(name = "Slurm Corp")
                            customerEdge(name = "Mom's Friendly Robot Company")
                        }
                    )
            }

            @Test
            fun `should not return customers from inaccessible workspace`() {
                client.graphql {
                    customers(workspaceId = preconditions.workspace.id!!.toInt(), first = 10) {
                        edges {
                            node { name }
                        }
                        totalCount
                    }
                }
                    .from(preconditions.zoidberg)
                    .executeAndVerifyEntityNotFoundError(
                        path = DgsConstants.QUERY.Customers,
                    )
            }
        }

        @Nested
        @DisplayName("Pagination")
        inner class Pagination {

            private fun EntitiesFactory.threeCustomers() = object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val c1 = customer(workspace = workspace, name = "Slurm Corp").also {
                    it.createdAt = MOCK_TIME.plusSeconds(100)
                    it.save()
                }
                val c2 = customer(workspace = workspace, name = "Mom's Friendly Robot Company").also {
                    it.createdAt = MOCK_TIME.plusSeconds(200)
                    it.save()
                }
                val c3 = customer(workspace = workspace, name = "Planet Express Competitors Inc").also {
                    it.createdAt = MOCK_TIME.plusSeconds(300)
                    it.save()
                }
            }

            @Test
            fun `should return first page with pageInfo`() {
                val testData = preconditions { threeCustomers() }
                client.graphql {
                    customers(workspaceId = testData.workspace.id!!.toInt(), first = 2) {
                        edges {
                            cursor
                            node { name }
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
                        "customers" to buildJsonObject {
                            putJsonArray("edges") {
                                add(buildJsonObject {
                                    put("cursor", encodeCursor(testData.c1.createdAt!!))
                                    put("node", buildJsonObject { put("name", "Slurm Corp") })
                                })
                                add(buildJsonObject {
                                    put("cursor", encodeCursor(testData.c2.createdAt!!))
                                    put("node", buildJsonObject { put("name", "Mom's Friendly Robot Company") })
                                })
                            }
                            put("pageInfo", buildJsonObject {
                                put("startCursor", encodeCursor(testData.c1.createdAt!!))
                                put("endCursor", encodeCursor(testData.c2.createdAt!!))
                                put("hasPreviousPage", false)
                                put("hasNextPage", true)
                            })
                            put("totalCount", 3)
                        }
                    )
            }

            @Test
            fun `should return second page using after cursor`() {
                val testData = preconditions { threeCustomers() }
                val afterCursor = encodeCursor(testData.c2.createdAt!!)
                client.graphql {
                    customers(workspaceId = testData.workspace.id!!.toInt(), first = 10, after = afterCursor) {
                        edges {
                            cursor
                            node { name }
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
                        "customers" to buildJsonObject {
                            putJsonArray("edges") {
                                add(buildJsonObject {
                                    put("cursor", encodeCursor(testData.c3.createdAt!!))
                                    put("node", buildJsonObject { put("name", "Planet Express Competitors Inc") })
                                })
                            }
                            put("pageInfo", buildJsonObject {
                                put("startCursor", encodeCursor(testData.c3.createdAt!!))
                                put("endCursor", encodeCursor(testData.c3.createdAt!!))
                                put("hasPreviousPage", true)
                                put("hasNextPage", false)
                            })
                            put("totalCount", 3)
                        }
                    )
            }

            @Test
            fun `should reject first greater than 500`() {
                client.graphql {
                    customers(workspaceId = preconditions.workspace.id!!.toInt(), first = 501) {
                        edges {
                            node { name }
                        }
                    }
                }
                    .from(preconditions.fry)
                    .executeAndVerifyValidationError(
                        violationPath = "first",
                        error = "MaxConstraintViolated",
                        message = "must be less than or equal to 500",
                        path = DgsConstants.QUERY.Customers,
                        params = mapOf("value" to "500"),
                    )
            }

            @Test
            fun `should reject first less than 1`() {
                client.graphql {
                    customers(workspaceId = preconditions.workspace.id!!.toInt(), first = 0) {
                        edges {
                            node { name }
                        }
                    }
                }
                    .from(preconditions.fry)
                    .executeAndVerifyValidationError(
                        violationPath = "first",
                        error = "MinConstraintViolated",
                        message = "must be greater than or equal to 1",
                        path = DgsConstants.QUERY.Customers,
                        params = mapOf("value" to "1"),
                    )
            }
        }
    }

    @Nested
    @DisplayName("Workspace-nested customers query")
    inner class WorkspaceNestedQuery {

        @Nested
        @DisplayName("Pagination")
        inner class Pagination {

            private fun EntitiesFactory.threeCustomers() = object {
                val fry = fry()
                val workspace = workspace(owner = fry)
                val customer1 = customer(workspace = workspace, name = "MomCorp").also {
                    it.createdAt = MOCK_TIME.plusSeconds(100)
                    it.save()
                }
                val customer2 = customer(workspace = workspace, name = "Planet Express").also {
                    it.createdAt = MOCK_TIME.plusSeconds(200)
                    it.save()
                }
                val customer3 = customer(workspace = workspace, name = "Slurm Inc").also {
                    it.createdAt = MOCK_TIME.plusSeconds(300)
                    it.save()
                }
            }

            @Test
            fun `should return first page with pageInfo`() {
                val testData = preconditions { threeCustomers() }
                client.graphql {
                    workspace(id = testData.workspace.id!!.toInt()) {
                        customers(first = 2) {
                            edges {
                                cursor
                                node { name }
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
                }
                    .from(testData.fry)
                    .executeAndVerifyResponse(
                        "workspace" to buildJsonObject {
                            put("customers", buildJsonObject {
                                putJsonArray("edges") {
                                    add(buildJsonObject {
                                        put("cursor", encodeCursor(testData.customer1.createdAt!!))
                                        put("node", buildJsonObject { put("name", "MomCorp") })
                                    })
                                    add(buildJsonObject {
                                        put("cursor", encodeCursor(testData.customer2.createdAt!!))
                                        put("node", buildJsonObject { put("name", "Planet Express") })
                                    })
                                }
                                put("pageInfo", buildJsonObject {
                                    put("startCursor", encodeCursor(testData.customer1.createdAt!!))
                                    put("endCursor", encodeCursor(testData.customer2.createdAt!!))
                                    put("hasPreviousPage", false)
                                    put("hasNextPage", true)
                                })
                                put("totalCount", 3)
                            })
                        }
                    )
            }

            @Test
            fun `should return second page using after cursor`() {
                val testData = preconditions { threeCustomers() }
                val afterCursor = encodeCursor(testData.customer2.createdAt!!)
                client.graphql {
                    workspace(id = testData.workspace.id!!.toInt()) {
                        customers(first = 10, after = afterCursor) {
                            edges {
                                cursor
                                node { name }
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
                }
                    .from(testData.fry)
                    .executeAndVerifyResponse(
                        "workspace" to buildJsonObject {
                            put("customers", buildJsonObject {
                                putJsonArray("edges") {
                                    add(buildJsonObject {
                                        put("cursor", encodeCursor(testData.customer3.createdAt!!))
                                        put("node", buildJsonObject { put("name", "Slurm Inc") })
                                    })
                                }
                                put("pageInfo", buildJsonObject {
                                    put("startCursor", encodeCursor(testData.customer3.createdAt!!))
                                    put("endCursor", encodeCursor(testData.customer3.createdAt!!))
                                    put("hasPreviousPage", true)
                                    put("hasNextPage", false)
                                })
                                put("totalCount", 3)
                            })
                        }
                    )
            }

            @Test
            fun `should return empty connection when no customers exist`() {
                val testData = preconditions {
                    object {
                        val fry = fry()
                        val workspace = workspace(owner = fry)
                    }
                }
                client.graphql {
                    workspace(id = testData.workspace.id!!.toInt()) {
                        customers(first = 10) {
                            edges {
                                cursor
                                node { name }
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
                }
                    .from(testData.fry)
                    .executeAndVerifyResponse(
                        "workspace" to buildJsonObject {
                            put("customers", emptyCustomersConnection(
                                hasPreviousPage = false,
                                hasNextPage = false,
                                totalCount = 0,
                            ))
                        }
                    )
            }

            @Test
            fun `should return empty page when cursor is past all items`() {
                val testData = preconditions {
                    object {
                        val fry = fry()
                        val workspace = workspace(owner = fry)
                        val customer1 = customer(workspace = workspace, name = "MomCorp").also {
                            it.createdAt = MOCK_TIME.plusSeconds(100)
                            it.save()
                        }
                    }
                }
                val afterCursor = encodeCursor(testData.customer1.createdAt!!)
                client.graphql {
                    workspace(id = testData.workspace.id!!.toInt()) {
                        customers(first = 10, after = afterCursor) {
                            edges {
                                cursor
                                node { name }
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
                }
                    .from(testData.fry)
                    .executeAndVerifyResponse(
                        "workspace" to buildJsonObject {
                            put("customers", emptyCustomersConnection(
                                hasPreviousPage = true,
                                hasNextPage = false,
                                totalCount = 1,
                            ))
                        }
                    )
            }

            @Test
            fun `should return all items when first equals total count`() {
                val testData = preconditions { threeCustomers() }
                client.graphql {
                    workspace(id = testData.workspace.id!!.toInt()) {
                        customers(first = 3) {
                            edges {
                                node { name }
                            }
                            pageInfo {
                                hasNextPage
                                hasPreviousPage
                            }
                            totalCount
                        }
                    }
                }
                    .from(testData.fry)
                    .executeAndVerifyResponse(
                        "workspace" to buildJsonObject {
                            put("customers", buildJsonObject {
                                putJsonArray("edges") {
                                    customerEdge(name = "MomCorp")
                                    customerEdge(name = "Planet Express")
                                    customerEdge(name = "Slurm Inc")
                                }
                                put("pageInfo", buildJsonObject {
                                    put("hasPreviousPage", false)
                                    put("hasNextPage", false)
                                })
                                put("totalCount", 3)
                            })
                        }
                    )
            }

            @Test
            fun `should not include customers from other workspaces`() {
                val testData = preconditions {
                    object {
                        val fry = fry()
                        val workspace = workspace(owner = fry)
                        val customer1 = customer(workspace = workspace, name = "MomCorp").also {
                            it.createdAt = MOCK_TIME.plusSeconds(100)
                            it.save()
                        }
                    }.also {
                        val otherWorkspace = workspace(owner = zoidberg())
                        customer(workspace = otherWorkspace, name = "Fishy Joe's")
                    }
                }
                client.graphql {
                    workspace(id = testData.workspace.id!!.toInt()) {
                        customers(first = 10) {
                            edges {
                                node { name }
                            }
                            totalCount
                        }
                    }
                }
                    .from(testData.fry)
                    .executeAndVerifyResponse(
                        "workspace" to buildJsonObject {
                            put("customers", buildJsonObject {
                                putJsonArray("edges") {
                                    customerEdge(name = "MomCorp")
                                }
                                put("totalCount", 1)
                            })
                        }
                    )
            }

            @Test
            fun `should order by createdAt ascending`() {
                val testData = preconditions {
                    object {
                        val fry = fry()
                        val workspace = workspace(owner = fry)
                    }.also {
                        customer(workspace = it.workspace, name = "Slurm Inc").also { c ->
                            c.createdAt = MOCK_TIME.plusSeconds(300)
                            c.save()
                        }
                        customer(workspace = it.workspace, name = "MomCorp").also { c ->
                            c.createdAt = MOCK_TIME.plusSeconds(100)
                            c.save()
                        }
                        customer(workspace = it.workspace, name = "Planet Express").also { c ->
                            c.createdAt = MOCK_TIME.plusSeconds(200)
                            c.save()
                        }
                    }
                }
                client.graphql {
                    workspace(id = testData.workspace.id!!.toInt()) {
                        customers(first = 10) {
                            edges {
                                node { name }
                            }
                        }
                    }
                }
                    .from(testData.fry)
                    .executeAndVerifyResponse(
                        "workspace" to buildJsonObject {
                            put("customers", buildJsonObject {
                                putJsonArray("edges") {
                                    customerEdge(name = "MomCorp")
                                    customerEdge(name = "Planet Express")
                                    customerEdge(name = "Slurm Inc")
                                }
                            })
                        }
                    )
            }
        }

        @Nested
        @DisplayName("Customer Fields")
        inner class CustomerFields {
            @Test
            fun `should return all customer fields`() {
                val testData = preconditions {
                    object {
                        val fry = fry()
                        val workspace = workspace(owner = fry)
                        val customer = customer(workspace = workspace, name = "MomCorp")
                    }
                }
                client.graphql {
                    workspace(id = testData.workspace.id!!.toInt()) {
                        customers(first = 10) {
                            edges {
                                node {
                                    id
                                    name
                                }
                            }
                        }
                    }
                }
                    .from(testData.fry)
                    .executeAndVerifyResponse(
                        "workspace" to buildJsonObject {
                            put("customers", buildJsonObject {
                                putJsonArray("edges") {
                                    add(buildJsonObject {
                                        put("node", buildJsonObject {
                                            put("id", testData.customer.id!!.toInt())
                                            put("name", "MomCorp")
                                        })
                                    })
                                }
                            })
                        }
                    )
            }
        }
    }
}

private fun customersConnection(
    totalCount: Int? = null,
    hasPreviousPage: Boolean = false,
    hasNextPage: Boolean = false,
    includePageInfo: Boolean = false,
    edgesSpec: (kotlinx.serialization.json.JsonArrayBuilder.() -> Unit)? = null,
): JsonElement = buildJsonObject {
    putJsonArray("edges") {
        if (edgesSpec != null) {
            edgesSpec()
        }
    }
    if (totalCount != null) {
        put("totalCount", totalCount)
    }
    if (includePageInfo) {
        put("pageInfo", buildJsonObject {
            put("hasPreviousPage", hasPreviousPage)
            put("hasNextPage", hasNextPage)
            if (edgesSpec == null) {
                put("startCursor", null as String?)
                put("endCursor", null as String?)
            }
        })
    }
}

private fun kotlinx.serialization.json.JsonArrayBuilder.customerEdge(name: String) {
    add(buildJsonObject {
        put("node", buildJsonObject { put("name", name) })
    })
}

private fun emptyCustomersConnection(
    totalCount: Int,
    hasPreviousPage: Boolean,
    hasNextPage: Boolean,
): JsonElement = buildJsonObject {
    putJsonArray("edges") {}
    put("pageInfo", buildJsonObject {
        put("startCursor", null as String?)
        put("endCursor", null as String?)
        put("hasPreviousPage", hasPreviousPage)
        put("hasNextPage", hasNextPage)
    })
    put("totalCount", totalCount)
}
