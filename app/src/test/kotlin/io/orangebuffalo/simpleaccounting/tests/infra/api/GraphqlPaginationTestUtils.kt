package io.orangebuffalo.simpleaccounting.tests.infra.api

import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import io.orangebuffalo.simpleaccounting.infra.graphql.client.QueryProjection
import io.orangebuffalo.simpleaccounting.infra.graphql.connections.encodeCursor
import io.orangebuffalo.simpleaccounting.tests.infra.database.EntitiesFactory
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.time.Instant

/**
 * Base interface for parameterized GraphQL pagination test cases.
 *
 * Use [standardPaginationTestCases] to generate the standard set of pagination test cases,
 * and [paginationValidationTestCases] for input validation (first > 500, first < 1).
 *
 * Execute via [executePaginationTestCase].
 */
sealed interface GraphqlPaginationTestCase {
    val description: String
}

/**
 * Specification that each entity test provides to describe how to set up preconditions,
 * build queries, and format expected responses for pagination tests.
 */
interface GraphqlPaginationTestSpec {
    val client: ApiTestClient

    /**
     * Creates 3 entities with known names and staggered `createdAt` timestamps.
     * Returns the owner user and [ThreeEntitiesData] containing entity names and timestamps.
     */
    fun EntitiesFactory.createThreeEntities(): ThreeEntitiesData

    /**
     * Creates a single entity owned by the given user.
     * Returns the entity's `createdAt` timestamp after saving.
     */
    fun EntitiesFactory.createSingleEntity(owner: PlatformUser): Instant

    /**
     * Creates entities belonging to a different owner (for isolation tests).
     */
    fun EntitiesFactory.createOtherOwnerEntities()

    /**
     * Builds the GraphQL query with the given pagination parameters.
     * The query should request edges with cursor and node { name }, plus pageInfo and totalCount
     * as specified by the [queryFields] parameter.
     */
    fun buildQuery(
        workspaceId: Long? = null,
        first: Int,
        after: String? = null,
        queryFields: PaginationQueryFields,
    ): QueryProjection.() -> QueryProjection

    /**
     * Wraps the connection JSON response in the appropriate response structure.
     * For top-level queries (workspaces), the key is just "workspaces".
     * For nested queries (customers, documents), it's nested under "workspace".
     */
    fun wrapResponse(connectionJson: JsonElement): Pair<String, JsonElement>

    /**
     * The GraphQL path used for validation error assertions (e.g., `DgsConstants.QUERY.Workspaces`).
     */
    val validationErrorPath: String
}

/**
 * Data returned by [GraphqlPaginationTestSpec.createThreeEntities].
 */
data class ThreeEntitiesData(
    val owner: PlatformUser,
    val workspaceId: Long?,
    val entity1Name: String,
    val entity1CreatedAt: Instant,
    val entity2Name: String,
    val entity2CreatedAt: Instant,
    val entity3Name: String,
    val entity3CreatedAt: Instant,
)

/**
 * Specifies which fields the pagination query should request.
 */
data class PaginationQueryFields(
    val includeCursor: Boolean = false,
    val includePageInfo: Boolean = false,
    val includeFullPageInfo: Boolean = false,
    val includeTotalCount: Boolean = false,
)

data class FirstPageTestCase(
    override val description: String = "should return first page with pageInfo",
) : GraphqlPaginationTestCase {
    override fun toString() = description
}

data class SecondPageTestCase(
    override val description: String = "should return second page using after cursor",
) : GraphqlPaginationTestCase {
    override fun toString() = description
}

data class EmptyConnectionTestCase(
    override val description: String = "should return empty connection when no entities exist",
) : GraphqlPaginationTestCase {
    override fun toString() = description
}

data class CursorPastAllItemsTestCase(
    override val description: String = "should return empty page when cursor is past all items",
) : GraphqlPaginationTestCase {
    override fun toString() = description
}

data class AllItemsTestCase(
    override val description: String = "should return all items when first equals total count",
) : GraphqlPaginationTestCase {
    override fun toString() = description
}

data class OtherOwnerExcludedTestCase(
    override val description: String = "should not include entities from other owners in pagination",
) : GraphqlPaginationTestCase {
    override fun toString() = description
}

data class OrderByCreatedAtTestCase(
    override val description: String = "should order by createdAt ascending",
) : GraphqlPaginationTestCase {
    override fun toString() = description
}

data class FirstGreaterThan500TestCase(
    override val description: String = "should reject first greater than 500",
) : GraphqlPaginationTestCase {
    override fun toString() = description
}

data class FirstLessThan1TestCase(
    override val description: String = "should reject first less than 1",
) : GraphqlPaginationTestCase {
    override fun toString() = description
}

/**
 * Generates the standard 7 pagination test cases applicable to all connection types.
 */
fun standardPaginationTestCases(): List<GraphqlPaginationTestCase> = listOf(
    FirstPageTestCase(),
    SecondPageTestCase(),
    EmptyConnectionTestCase(),
    CursorPastAllItemsTestCase(),
    AllItemsTestCase(),
    OtherOwnerExcludedTestCase(),
    OrderByCreatedAtTestCase(),
)

/**
 * Generates the 2 validation test cases for the `first` parameter.
 */
fun paginationValidationTestCases(): List<GraphqlPaginationTestCase> = listOf(
    FirstGreaterThan500TestCase(),
    FirstLessThan1TestCase(),
)

/**
 * Executes a pagination test case using the given spec and the base class's preconditions helper.
 */
fun executePaginationTestCase(
    testCase: GraphqlPaginationTestCase,
    spec: GraphqlPaginationTestSpec,
    preconditions: (EntitiesFactory.() -> Any) -> Any,
) {
    when (testCase) {
        is FirstPageTestCase -> executeFirstPageTest(spec, preconditions)
        is SecondPageTestCase -> executeSecondPageTest(spec, preconditions)
        is EmptyConnectionTestCase -> executeEmptyConnectionTest(spec, preconditions)
        is CursorPastAllItemsTestCase -> executeCursorPastAllItemsTest(spec, preconditions)
        is AllItemsTestCase -> executeAllItemsTest(spec, preconditions)
        is OtherOwnerExcludedTestCase -> executeOtherOwnerExcludedTest(spec, preconditions)
        is OrderByCreatedAtTestCase -> executeOrderByCreatedAtTest(spec, preconditions)
        is FirstGreaterThan500TestCase -> executeFirstGreaterThan500Test(spec, preconditions)
        is FirstLessThan1TestCase -> executeFirstLessThan1Test(spec, preconditions)
    }
}

private fun executeFirstPageTest(
    spec: GraphqlPaginationTestSpec,
    preconditions: (EntitiesFactory.() -> Any) -> Any,
) {
    val testData = preconditions { spec.run { createThreeEntities() } } as ThreeEntitiesData
    spec.client.graphql(
        spec.buildQuery(
            workspaceId = testData.workspaceId,
            first = 2,
            queryFields = PaginationQueryFields(
                includeCursor = true,
                includeFullPageInfo = true,
                includeTotalCount = true,
            ),
        )
    )
        .from(testData.owner)
        .executeAndVerifyResponse(
            spec.wrapResponse(
                buildJsonObject {
                    putJsonArray("edges") {
                        add(buildJsonObject {
                            put("cursor", encodeCursor(testData.entity1CreatedAt))
                            put("node", buildJsonObject { put("name", testData.entity1Name) })
                        })
                        add(buildJsonObject {
                            put("cursor", encodeCursor(testData.entity2CreatedAt))
                            put("node", buildJsonObject { put("name", testData.entity2Name) })
                        })
                    }
                    put("pageInfo", buildJsonObject {
                        put("startCursor", encodeCursor(testData.entity1CreatedAt))
                        put("endCursor", encodeCursor(testData.entity2CreatedAt))
                        put("hasPreviousPage", false)
                        put("hasNextPage", true)
                    })
                    put("totalCount", 3)
                }
            )
        )
}

private fun executeSecondPageTest(
    spec: GraphqlPaginationTestSpec,
    preconditions: (EntitiesFactory.() -> Any) -> Any,
) {
    val testData = preconditions { spec.run { createThreeEntities() } } as ThreeEntitiesData
    val afterCursor = encodeCursor(testData.entity2CreatedAt)
    spec.client.graphql(
        spec.buildQuery(
            workspaceId = testData.workspaceId,
            first = 10,
            after = afterCursor,
            queryFields = PaginationQueryFields(
                includeCursor = true,
                includeFullPageInfo = true,
                includeTotalCount = true,
            ),
        )
    )
        .from(testData.owner)
        .executeAndVerifyResponse(
            spec.wrapResponse(
                buildJsonObject {
                    putJsonArray("edges") {
                        add(buildJsonObject {
                            put("cursor", encodeCursor(testData.entity3CreatedAt))
                            put("node", buildJsonObject { put("name", testData.entity3Name) })
                        })
                    }
                    put("pageInfo", buildJsonObject {
                        put("startCursor", encodeCursor(testData.entity3CreatedAt))
                        put("endCursor", encodeCursor(testData.entity3CreatedAt))
                        put("hasPreviousPage", true)
                        put("hasNextPage", false)
                    })
                    put("totalCount", 3)
                }
            )
        )
}

private fun executeEmptyConnectionTest(
    spec: GraphqlPaginationTestSpec,
    preconditions: (EntitiesFactory.() -> Any) -> Any,
) {
    data class EmptyTestData(val owner: PlatformUser, val workspaceId: Long?)
    val testData = preconditions {
        val owner = fry()
        val workspace = workspace(owner = owner)
        EmptyTestData(owner, workspace.id)
    } as EmptyTestData
    spec.client.graphql(
        spec.buildQuery(
            workspaceId = testData.workspaceId,
            first = 10,
            queryFields = PaginationQueryFields(
                includeCursor = true,
                includeFullPageInfo = true,
                includeTotalCount = true,
            ),
        )
    )
        .from(testData.owner)
        .executeAndVerifyResponse(
            spec.wrapResponse(
                emptyConnection(
                    totalCount = 0,
                    hasPreviousPage = false,
                    hasNextPage = false,
                )
            )
        )
}

private fun executeCursorPastAllItemsTest(
    spec: GraphqlPaginationTestSpec,
    preconditions: (EntitiesFactory.() -> Any) -> Any,
) {
    data class SingleEntityTestData(val owner: PlatformUser, val workspaceId: Long?, val entityCreatedAt: Instant)
    val testData = preconditions {
        val owner = fry()
        val workspace = workspace(owner = owner)
        val createdAt = spec.run { createSingleEntity(owner) }
        SingleEntityTestData(owner, workspace.id, createdAt)
    } as SingleEntityTestData
    val afterCursor = encodeCursor(testData.entityCreatedAt)
    spec.client.graphql(
        spec.buildQuery(
            workspaceId = testData.workspaceId,
            first = 10,
            after = afterCursor,
            queryFields = PaginationQueryFields(
                includeCursor = true,
                includeFullPageInfo = true,
                includeTotalCount = true,
            ),
        )
    )
        .from(testData.owner)
        .executeAndVerifyResponse(
            spec.wrapResponse(
                emptyConnection(
                    totalCount = 1,
                    hasPreviousPage = true,
                    hasNextPage = false,
                )
            )
        )
}

private fun executeAllItemsTest(
    spec: GraphqlPaginationTestSpec,
    preconditions: (EntitiesFactory.() -> Any) -> Any,
) {
    val testData = preconditions { spec.run { createThreeEntities() } } as ThreeEntitiesData
    spec.client.graphql(
        spec.buildQuery(
            workspaceId = testData.workspaceId,
            first = 3,
            queryFields = PaginationQueryFields(
                includePageInfo = true,
                includeTotalCount = true,
            ),
        )
    )
        .from(testData.owner)
        .executeAndVerifyResponse(
            spec.wrapResponse(
                buildJsonObject {
                    putJsonArray("edges") {
                        simpleEdge(testData.entity1Name)
                        simpleEdge(testData.entity2Name)
                        simpleEdge(testData.entity3Name)
                    }
                    put("pageInfo", buildJsonObject {
                        put("hasPreviousPage", false)
                        put("hasNextPage", false)
                    })
                    put("totalCount", 3)
                }
            )
        )
}

private fun executeOtherOwnerExcludedTest(
    spec: GraphqlPaginationTestSpec,
    preconditions: (EntitiesFactory.() -> Any) -> Any,
) {
    data class IsolationTestData(val owner: PlatformUser, val workspaceId: Long?, val entityName: String)
    val testData = preconditions {
        val owner = fry()
        val workspace = workspace(owner = owner)
        val createdAt = spec.run { createSingleEntity(owner) }
        spec.run { createOtherOwnerEntities() }
        IsolationTestData(owner, workspace.id, getLastCreatedEntityName(spec, owner))
    } as IsolationTestData
    spec.client.graphql(
        spec.buildQuery(
            workspaceId = testData.workspaceId,
            first = 10,
            queryFields = PaginationQueryFields(
                includeTotalCount = true,
            ),
        )
    )
        .from(testData.owner)
        .executeAndVerifyResponse(
            spec.wrapResponse(
                buildJsonObject {
                    putJsonArray("edges") {
                        simpleEdge(testData.entityName)
                    }
                    put("totalCount", 1)
                }
            )
        )
}

private fun executeOrderByCreatedAtTest(
    spec: GraphqlPaginationTestSpec,
    preconditions: (EntitiesFactory.() -> Any) -> Any,
) {
    val testData = preconditions { spec.run { createThreeEntities() } } as ThreeEntitiesData
    spec.client.graphql(
        spec.buildQuery(
            workspaceId = testData.workspaceId,
            first = 10,
            queryFields = PaginationQueryFields(),
        )
    )
        .from(testData.owner)
        .executeAndVerifyResponse(
            spec.wrapResponse(
                buildJsonObject {
                    putJsonArray("edges") {
                        simpleEdge(testData.entity1Name)
                        simpleEdge(testData.entity2Name)
                        simpleEdge(testData.entity3Name)
                    }
                }
            )
        )
}

private fun executeFirstGreaterThan500Test(
    spec: GraphqlPaginationTestSpec,
    preconditions: (EntitiesFactory.() -> Any) -> Any,
) {
    data class SimpleOwnerData(val owner: PlatformUser)
    val testData = preconditions {
        SimpleOwnerData(fry())
    } as SimpleOwnerData
    spec.client.graphql(
        spec.buildQuery(
            first = 501,
            queryFields = PaginationQueryFields(),
        )
    )
        .from(testData.owner)
        .executeAndVerifyValidationError(
            violationPath = "first",
            error = "MaxConstraintViolated",
            message = "must be less than or equal to 500",
            path = spec.validationErrorPath,
            params = mapOf("value" to "500"),
        )
}

private fun executeFirstLessThan1Test(
    spec: GraphqlPaginationTestSpec,
    preconditions: (EntitiesFactory.() -> Any) -> Any,
) {
    data class SimpleOwnerData(val owner: PlatformUser)
    val testData = preconditions {
        SimpleOwnerData(fry())
    } as SimpleOwnerData
    spec.client.graphql(
        spec.buildQuery(
            first = 0,
            queryFields = PaginationQueryFields(),
        )
    )
        .from(testData.owner)
        .executeAndVerifyValidationError(
            violationPath = "first",
            error = "MinConstraintViolated",
            message = "must be greater than or equal to 1",
            path = spec.validationErrorPath,
            params = mapOf("value" to "1"),
        )
}

private fun emptyConnection(
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

private fun kotlinx.serialization.json.JsonArrayBuilder.simpleEdge(name: String) {
    add(buildJsonObject {
        put("node", buildJsonObject { put("name", name) })
    })
}

private fun getLastCreatedEntityName(
    spec: GraphqlPaginationTestSpec,
    owner: PlatformUser,
): String {
    // This is determined by the spec's createSingleEntity - the name is entity-specific
    // We need to know the name. The spec should provide it.
    // Let's re-think: the OtherOwnerExcluded test creates one entity for the owner,
    // and the spec knows the name. We need a way to get that name.
    throw UnsupportedOperationException("Should not be called directly")
}
