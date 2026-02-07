package io.orangebuffalo.simpleaccounting.tests.infra.api

import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import io.orangebuffalo.simpleaccounting.business.workspaces.Workspace
import io.orangebuffalo.simpleaccounting.infra.TimeService
import io.orangebuffalo.simpleaccounting.tests.infra.database.EntitiesFactory
import io.orangebuffalo.simpleaccounting.tests.infra.database.EntitiesFactoryInfra
import io.orangebuffalo.simpleaccounting.tests.infra.utils.mockCurrentTime
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jdbc.core.JdbcAggregateTemplate
import org.springframework.transaction.PlatformTransactionManager
import java.util.stream.Stream

/**
 * Annotation to mark classes and interfaces that are part of the Filtering Test API DSL.
 */
@DslMarker
annotation class FilteringTestApiDslMarker

/**
 * Generates a collection of FilteringApiTestCase instances based on the provided builder function.
 *
 * To be used in the companion object method "createTestCases" of a test class that extend [FilteringApiTestBase].
 *
 * ```
 * @SimpleAccountingIntegrationTest
 * class AggregateApiIT : AbstractFilteringApiTest() {
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
interface FilteringApiTestCasesBuilder<T : Any> {
    /**
     * Base URL for the filtering API. Depending on the value of [workspaceBasedUrl],
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
     * Configures how response entities are matched to the preconditions entities,
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
interface EntityMatcher<T : Any> {
    /**
     * Configures the field that is extracted from the response in order to construct an
     * actual result entity. It is then matched against the same extraction from the
     * preconditions entity as defined by [entityField].
     */
    fun responseFieldToEntityField(responseField: String, entityField: (T) -> Any?)
}

/**
 * Configures filtering specification.
 */
@FilteringTestApiDslMarker
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
abstract class FilteringApiTestCase {
    abstract fun execute(
        client: ApiTestClient,
        entitiesFactoryInfra: EntitiesFactoryInfra,
        jdbcAggregateTemplate: JdbcAggregateTemplate
    )
}

/**
 * Base class for integration tests that test the filtering API.
 * Must be a nested test in the Spring Boot test.
 * See [generateFilteringApiTests] for more details and usage guidelines.
 */
@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class FilteringApiTestBase {

    @Autowired
    lateinit var client: ApiTestClient

    @Autowired
    lateinit var timeService: TimeService

    @Autowired
    lateinit var aggregateTemplate: JdbcAggregateTemplate

    @Autowired
    lateinit var platformTransactionManager: PlatformTransactionManager

    @ParameterizedTest
    @ArgumentsSource(FilteringApiTestArgumentsProvider::class)
    fun testFilteringApi(testCase: FilteringApiTestCase) {
        mockCurrentTime(timeService)
        testCase.execute(
            client = client,
            entitiesFactoryInfra = EntitiesFactoryInfra(
                platformTransactionManager,
                aggregateTemplate
            ),
            jdbcAggregateTemplate = aggregateTemplate
        )
    }

    abstract fun createTestCases(): Collection<FilteringApiTestCase>

    class FilteringApiTestArgumentsProvider : ArgumentsProvider {
        override fun provideArguments(extensionContext: ExtensionContext): Stream<out Arguments> {
            val testInstance = extensionContext.testInstance.get() as FilteringApiTestBase
            return testInstance.createTestCases()
                .stream()
                .map { Arguments.of(it) }
        }
    }
}
