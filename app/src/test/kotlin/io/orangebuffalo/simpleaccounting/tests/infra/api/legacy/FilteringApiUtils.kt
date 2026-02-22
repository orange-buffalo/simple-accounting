package io.orangebuffalo.simpleaccounting.tests.infra.api.legacy

import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import io.orangebuffalo.simpleaccounting.business.workspaces.Workspace
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.tests.infra.database.EntitiesFactory
import io.orangebuffalo.simpleaccounting.tests.infra.database.EntitiesFactoryInfra
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient

/**
 * Annotation to mark classes and interfaces that are part of the Filtering Test API DSL.
 */
@DslMarker
@Deprecated("Use new API from parent package")
annotation class FilteringTestApiDslMarker

/**
 * Generates a collection of FilteringApiTestCase instances based on the provided builder function.
 *
 * To be used in the companion object method "createTestCases" of a test class that extend [AbstractFilteringApiTest].
 *
 * ```
 *
 * class AggregateApiIT : AbstractFilteringApiTest() : SaIntegrationTestBase() {
 *
 *      companion object {
 *          @Suppress("unused")
 *          @JvmStatic
 *          fun createTestCases() = generateFilteringApiTests<Aggregate> {
 *             ...
 *          }
 *      }
 * }
 * ```
 * @param init Specification of the filtering test cases.
 */
@Deprecated("Use new API from parent package")
inline fun <reified T : Any> generateFilteringApiTests(
    init: FilteringApiTestCasesBuilder<T>.() -> Unit
): Collection<FilteringApiTestCase> {
    val testCasesBuilder = FilteringApiTestCasesBuilderImpl(T::class)
    init(testCasesBuilder)
    return testCasesBuilder.buildTestCases()
}

/**
 * Interface for building filtering test cases.
 */
@FilteringTestApiDslMarker
@Deprecated("Use new API from parent package")
interface FilteringApiTestCasesBuilder<T : Any> {
    /**
     * Base URL for the filtering API. Depending on the value of [workspaceBasedUrl]
     * the URL will be constructed as "/api/workspaces/{workspaceId}/{baseUrl}" or "/api/{baseUrl}".
     */
    var baseUrl: String

    /**
     * Whether the URL should be based on current workspace or on global namespace.
     *
     * By default, the value is `true`.
     * @see baseUrl
     */
    var workspaceBasedUrl: Boolean

    /**
     * Whether the test case should be executed as an admin user or a regular user.
     *
     * By default, the value is `false`, meaning that tests are executed as a regular user.
     */
    var executeAsAdmin: Boolean

    /**
     * Configures how response entities are matched to the preconditions entities
     * which is basically the fields mapping between the aggregate and the response.
     */
    fun entityMatcher(init: EntityMatcher<T>.() -> Unit)

    /**
     * Defines filtering specification (list of entities and how they react on different filters).
     */
    fun filtering(init: FilteringTestCaseBuilder<T>.() -> Unit)

    /**
     * Defines sorting specification (list of entities and how they react on different sorting options).
     */
    fun sorting(init: SortingTestCaseBuilder<T>.() -> Unit)
}

/**
 * Configures how response entities are matched to the preconditions entities.
 */
@FilteringTestApiDslMarker
@Deprecated("Use new API from parent package")
interface EntityMatcher<T : Any> {
    /**
     * Configures the fields that are extracted from the response in order to construct an
     * actual result entity. It is then matched against the same extraction from the
     * preconditions entity as defined by [entityFields].
     */
    fun responseFields(vararg fieldNames: String)

    /**
     * Configures the fields that are extracted from the preconditions entity in order to construct an
     * expected result entity. It is then matched against the same extraction from the
     * response as defined by [responseFields].
     *
     * The order of the fields should match the order of the fields in [responseFields].
     */
    fun entityFields(vararg fieldExtractor: (entity: T) -> Any?)
}

/**
 * Configures filtering specification.
 */
@FilteringTestApiDslMarker
@Deprecated("Use new API from parent package")
interface FilteringTestCaseBuilder<T : Any> {
    /**
     * Registers a new entity in the preconditions and defines
     * how it reacts on different filters.
     */
    fun entity(init: FilteringEntityBuilder<T>.() -> Unit)

    /**
     * Configures a preconditions entity and defines how it reacts on different filters.
     */
    @FilteringTestApiDslMarker
    @Deprecated("Use new API from parent package")
    interface FilteringEntityBuilder<T : Any> {
        /**
         * Adds an entity to the preconditions, and potentially created additionally required dependencies.
         * The returned entity is then expected to be provided in the API response, unless [skippedOn] configures
         * it to be excluded for particular filters.
         */
        fun createEntity(init: EntitiesRegistry.() -> T)

        /**
         * Registers expectation for the entity to be skipped when the specified filter is applied.
         * The filter is a fully defined query parameter, e.g. `id`[eq`]=123`.
         *
         * This method contributes to the test cases' creation. Each unique filter is
         * generating a test case. In addition, there is combined test case for all filters.
         *
         * The filters can contain placeholders, e.g. `id`[eq`]={ID}`. The placeholders are replaced
         * with the actual values extracted from the entity using [dynamicFilterReplacement].
         */
        fun skippedOn(filter: String)

        /**
         * Registers a dynamic filter replacement for the specified placeholder.
         * The placeholder is a part of the filter, e.g. `id`[eq`]={ID}`.
         */
        fun dynamicFilterReplacement(placeholder: String, valueExtractor: (T) -> Any?)
    }
}

/**
 * Configures sorting specification.
 */
@FilteringTestApiDslMarker
@Deprecated("Use new API from parent package")
interface SortingTestCaseBuilder<T : Any> {
    /**
     * Registers a new test case to execute sorting by the specified field in ascending order.
     */
    fun ascendingBy(apiField: String, init: EntitiesBuilder<T>.() -> Unit)

    /**
     * Registers a new test case for default (no query parameters) sorting.
     */
    fun default(init: EntitiesBuilder<T>.() -> Unit)

    /**
     * Configures a list of expected entities for the sorting test case.
     */
    @FilteringTestApiDslMarker
    @Deprecated("Use new API from parent package")
    interface EntitiesBuilder<T : Any> {
        /**
         * Registers a new entity in the preconditions and adds it to the
         * expected result of sorting test case. The order of this method calls
         * defines the expected order of the entities in the response.
         */
        fun goes(init: EntitiesRegistry.() -> T)
    }
}

/**
 * A registry of the entities that are used in the filtering test cases.
 * Can be used e.g. when additional entities are required for the target one
 * to be properly persisted, or when filtering concerns related entities.
 */
@FilteringTestApiDslMarker
@Deprecated("Use new API from parent package")
interface EntitiesRegistry {
    /**
     * The workspace for which tests are executed
     * (if [FilteringApiTestCasesBuilder.workspaceBasedUrl] is `true`).
     */
    val targetWorkspace: Workspace

    /**
     * The owner of the workspace for which tests are executed
     * (if [FilteringApiTestCasesBuilder.workspaceBasedUrl] is `true`).
     */
    val targetWorkspaceOwner: PlatformUser

    /**
     * The entities factory that can be used to create other entities.
     */
    val entitiesFactory: EntitiesFactory
}

/**
 * A filtering/sorting test case. Not intended to be used directly.
 * Use [generateFilteringApiTests] to create a collection of test cases and configure them.
 */
@Deprecated("Use new API from parent package")
abstract class FilteringApiTestCase {
    abstract fun execute(client: WebTestClient, entitiesFactoryInfra: EntitiesFactoryInfra)
}

/**
 * Base class for integration tests that test the filtering API.
 * See [generateFilteringApiTests] for more details and usage guidelines.
 */
// todo #229: investigate why random port is not applied when specified only on a base class
@MockitoSettings(strictness = Strictness.LENIENT)
@Deprecated("Use new API from parent package")
abstract class AbstractFilteringApiTest : SaIntegrationTestBase() {

    @Autowired
    lateinit var client: WebTestClient

    @ParameterizedTest
    @MethodSource("createTestCases")
    fun testFilteringApi(testCase: FilteringApiTestCase) {
        testCase.execute(client, entitiesFactoryInfra)
    }
}
