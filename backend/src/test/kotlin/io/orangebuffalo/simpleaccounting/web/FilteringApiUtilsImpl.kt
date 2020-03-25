package io.orangebuffalo.simpleaccounting.web

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsOnly
import assertk.assertions.isTrue
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import io.orangebuffalo.simpleaccounting.Prototypes
import io.orangebuffalo.simpleaccounting.support.kotlinEquals
import io.orangebuffalo.simpleaccounting.support.kotlinHashCode
import io.orangebuffalo.simpleaccounting.services.persistence.entities.PlatformUser
import io.orangebuffalo.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.simpleaccounting.utils.combine
import io.orangebuffalo.simpleaccounting.verifyOkAndBody
import mu.KotlinLogging
import org.springframework.test.web.reactive.server.WebTestClient
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

val logger = KotlinLogging.logger {}

class FilteringApiTestCasesBuilderImpl<T : Any>(
    entityType: KClass<T>
) : FilteringApiTestCasesBuilder<T> {

    override lateinit var baseUrl: String

    private lateinit var entityInitiator: EntitiesRegistry.() -> T
    private val entityMatcher: EntityMatcherImpl = EntityMatcherImpl()
    private val filteringBuilder: FilteringTestCaseBuilderImpl = FilteringTestCaseBuilderImpl()
    private val sortingBuilder: SortingTestCaseBuilderImpl = SortingTestCaseBuilderImpl()
    private val idEntityProperty = entityType.memberProperties.find { it.name == "id" }
        ?: throw IllegalStateException("$entityType does not have required ID property")

    override fun entityMatcher(init: EntityMatcher<T>.() -> Unit) {
        init(entityMatcher)
    }

    override fun defaultEntityProvider(init: EntitiesRegistry.() -> T) {
        entityInitiator = init
    }

    override fun filtering(init: FilteringTestCaseBuilder<T>.() -> Unit) {
        init(filteringBuilder)
    }

    override fun sorting(init: SortingTestCaseBuilder<T>.() -> Unit) {
        init(sortingBuilder)
    }

    fun buildTestCases(): Collection<FilteringApiTestCase> {
        val testCases: MutableSet<FilteringApiTestCase> = mutableSetOf()
        testCases.add(FilteringTestCase())
        addStandaloneFiltersTestCases(testCases)
        addFiltersCombinationsTestCases(testCases)

        testCases.add(SortingTestCase(sortingBuilder.defaultCase.entitiesInitiators))
        addSortingTestCases(testCases)

        logger.info { "Total generated test cases are: $testCases" }
        return testCases
    }

    private fun addSortingTestCases(testCases: MutableSet<FilteringApiTestCase>) {
        sortingBuilder.cases.asSequence()
            .flatMap { sortingSpec ->
                sequenceOf(
                    SortingTestCase(sortingSpec.entitiesInitiators, sortingSpec.asAscUrlFilter()),
                    SortingTestCase(sortingSpec.entitiesInitiators.reversed(), sortingSpec.asDescUrlFilter())
                )
            }
            .forEach { testCases.add(it) }
    }

    private fun addStandaloneFiltersTestCases(
        testCases: MutableSet<FilteringApiTestCase>
    ) {
        val filteringQueryParams = filteringBuilder.allFilters

        filteringQueryParams.forEach { queryParam ->
            testCases.add(
                FilteringTestCase().also { testCase -> testCase.addQueryParam(queryParam) }
            )
        }

        val filtersWithSorting: MutableList<List<String>> = mutableListOf()
        filtersWithSorting.add(filteringQueryParams)
        filtersWithSorting.add(sortingBuilder.allSortingQueryParameters)

        combine(filtersWithSorting, testCases.toCombinatorConsumer())
    }

    private fun addFiltersCombinationsTestCases(
        testCases: MutableSet<FilteringApiTestCase>
    ) {
        val filtersPerField = filteringBuilder.allFilters.asSequence()
            .groupBy { it.substring(0, it.indexOf('[')) }
            .values
            .toMutableList()

        filtersPerField.add(sortingBuilder.allSortingQueryParameters)

        combine(filtersPerField, testCases.toCombinatorConsumer())
    }

    private fun MutableSet<FilteringApiTestCase>.toCombinatorConsumer(): (List<String>) -> Unit {
        val testCases = this
        return { queryParams ->
            val testCase = FilteringTestCase()
            queryParams.forEach { testCase.addQueryParam(it) }
            testCases.add(testCase)
        }
    }

    private inner class EntityMatcherImpl : EntityMatcher<T> {
        lateinit var entityFieldExtractors: List<(entity: T) -> Any?>
        lateinit var responseFieldExtractors: List<(note: JsonNode) -> String>

        override fun entityFields(vararg fieldExtractor: (entity: T) -> Any?) {
            entityFieldExtractors = fieldExtractor.toList()
        }

        override fun responseFields(vararg fieldNames: String) {
            responseFieldExtractors = fieldNames.asSequence()
                .map(this::jsonFieldValueReader)
                .toList()
        }

        private fun jsonFieldValueReader(fieldName: String): (note: JsonNode) -> String = { node ->
            node.get(fieldName)?.asText() ?: ""
        }
    }

    private inner class FilteringTestCaseBuilderImpl : FilteringTestCaseBuilder<T> {
        val entitiesSpecs: MutableList<FilteringEntityBuilderImpl> = mutableListOf()

        val allFilters
            get() = entitiesSpecs.asSequence()
                .map { it.skippedOnFilters }
                .flatMap { it.asSequence() }
                .distinct()
                .sorted()
                .toList()

        override fun entity(init: FilteringTestCaseBuilder.FilteringEntityBuilder<T>.() -> Unit) {
            val entitySpec = this@FilteringApiTestCasesBuilderImpl.FilteringEntityBuilderImpl()
            entitiesSpecs.add(entitySpec)
            init(entitySpec)
        }
    }

    private inner class FilteringEntityBuilderImpl : FilteringTestCaseBuilder.FilteringEntityBuilder<T> {
        val skippedOnFilters: MutableList<String> = mutableListOf()
        lateinit var entityConfigurer: EntitiesRegistry.(entity: T) -> Unit

        override fun configure(init: EntitiesRegistry.(entity: T) -> Unit) {
            this.entityConfigurer = init
        }

        override fun skippedOn(filter: String) {
            skippedOnFilters.add(filter)
        }

        fun createEntity(entitiesRegistry: EntitiesRegistry): T {
            val entity = this@FilteringApiTestCasesBuilderImpl.entityInitiator(entitiesRegistry)
            entityConfigurer(entitiesRegistry, entity)
            entitiesRegistry.save(entity)
            return entity
        }

        fun isSkippedForUrl(url: String): Boolean = skippedOnFilters.any { url.contains(it) }
    }

    private inner class SortingTestCaseBuilderImpl : SortingTestCaseBuilder<T> {

        val cases: MutableList<SortingEntitiesBuilderImpl> = mutableListOf()
        val defaultCase: DefaultSortingEntitiesBuilderImpl =
            this@FilteringApiTestCasesBuilderImpl.DefaultSortingEntitiesBuilderImpl()

        val allSortingQueryParameters: List<String>
            get() = cases.map { it.asAscUrlFilter() }
                .union(cases.map { it.asDescUrlFilter() })
                .sorted()
                .toList()

        override fun ascendingBy(apiField: String, init: SortingTestCaseBuilder.EntitiesBuilder<T>.() -> Unit) {
            val entityBuilder = this@FilteringApiTestCasesBuilderImpl.SortingEntitiesBuilderImpl(apiField)
            init(entityBuilder)
            cases.add(entityBuilder)
        }

        override fun default(init: SortingTestCaseBuilder.EntitiesBuilder<T>.() -> Unit) {
            init(defaultCase)
        }
    }

    private inner class DefaultSortingEntitiesBuilderImpl : SortingTestCaseBuilder.EntitiesBuilder<T> {

        var entitiesInitiators: MutableList<EntitiesRegistry.(entity: T) -> Unit> = mutableListOf()

        override fun goes(init: EntitiesRegistry.(entity: T) -> Unit) {
            entitiesInitiators.add(init)
        }
    }

    private inner class SortingEntitiesBuilderImpl(val apiField: String) : SortingTestCaseBuilder.EntitiesBuilder<T> {

        var entitiesInitiators: MutableList<EntitiesRegistry.(entity: T) -> Unit> = mutableListOf()

        override fun goes(init: EntitiesRegistry.(entity: T) -> Unit) {
            entitiesInitiators.add(init)
        }

        fun asAscUrlFilter() = "sortBy=$apiField asc"
        fun asDescUrlFilter() = "sortBy=$apiField desc"
    }

    private inner class FilteringTestCase : AbstractFilteringApiTestCase(baseUrl) {

        private val filters: MutableList<String> = mutableListOf()
        private val equalsHashCodeProperties: Array<out (obj: FilteringTestCase) -> Any?>
            get() = arrayOf({ tc -> tc.filters })

        fun addQueryParam(filter: String) {
            url += if (filters.isEmpty()) "?" else "&"
            url += filter
            filters.add(filter)
        }

        override fun generateData(): List<Any> {
            filteringBuilder.entitiesSpecs.forEach { entitySpec ->
                val entity = entitySpec.createEntity(entitiesRegistry)
                if (!entitySpec.isSkippedForUrl(url)) {
                    expectedEntitiesInResponse.add(entity)
                }
            }

            url += (if (filters.isEmpty()) "?" else "&") + "limit=${filteringBuilder.entitiesSpecs.size}"

            return super.generateData()
        }

        override fun assertResults(responseEntitiesDesc: List<String>, expectedEntitiesDesc: List<String>) {
            assertThat(responseEntitiesDesc).containsOnly(*expectedEntitiesDesc.toTypedArray())
        }

        override fun equals(other: Any?) = kotlinEquals(other, equalsHashCodeProperties)
        override fun hashCode() = kotlinHashCode(equalsHashCodeProperties)
    }

    private inner class SortingTestCase(
        private val entitiesInitiators: List<EntitiesRegistry.(entity: T) -> Unit>,
        sortingQueryParam: String? = null
    ) : AbstractFilteringApiTestCase(baseUrl) {

        init {
            url += (if (sortingQueryParam == null) "?" else "?$sortingQueryParam&") + "limit=${entitiesInitiators.size}"
        }

        override fun generateData(): List<Any> {
            entitiesInitiators.forEach { sortingEntityInitiator ->
                val entity = this@FilteringApiTestCasesBuilderImpl.entityInitiator(entitiesRegistry)
                sortingEntityInitiator(entitiesRegistry, entity)
                entitiesRegistry.save(entity)
                expectedEntitiesInResponse.add(entity)
            }
            return super.generateData()
        }

        override fun assertResults(responseEntitiesDesc: List<String>, expectedEntitiesDesc: List<String>) {
            assertThat(responseEntitiesDesc).containsExactly(*expectedEntitiesDesc.toTypedArray())
        }
    }

    private abstract inner class AbstractFilteringApiTestCase(
        initialUrl: String
    ) : FilteringApiTestCase() {

        var url: String = initialUrl
        val entitiesRegistry: EntitiesRegistry = EntitiesRegistryImpl()
        val expectedEntitiesInResponse: MutableList<T> = mutableListOf()

        override fun generateData() = entitiesRegistry.entities

        override fun toString() = url

        abstract fun assertResults(responseEntitiesDesc: List<String>, expectedEntitiesDesc: List<String>)

        override fun execute(client: WebTestClient) {
            logger.info { "Executing $this" }
            client.get()
                .uri("/api/workspaces/${entitiesRegistry.workspace.id}/${url}")
                .verifyOkAndBody { body ->
                    val objectMapper = ObjectMapper()
                    val jsonResponse = objectMapper.readTree(body)

                    val data = jsonResponse.get("data")
                    assertThat(data.isArray).isTrue()

                    val responseEntities = (data as ArrayNode).asSequence()
                        .map { node ->
                            val id = node.get("id").asText()
                            val userFields = entityMatcher.responseFieldExtractors.asSequence()
                                .map { extractor -> extractor(node) }
                                .joinToString(", ")
                            "$id ($userFields)"
                        }
                        .toList()

                    val expectedEntities = expectedEntitiesInResponse.asSequence()
                        .map { entity ->
                            val id = idEntityProperty.get(entity)
                            val userFields = entityMatcher.entityFieldExtractors.asSequence()
                                .map { extractor -> extractor(entity) }
                                .map { it?.toString() ?: "" }
                                .joinToString(", ")
                            "$id ($userFields)"
                        }
                        .toList()

                    assertResults(responseEntities, expectedEntities)
                }
        }
    }

    private class EntitiesRegistryImpl : EntitiesRegistry {

        override val workspaceOwner: PlatformUser = Prototypes.platformUser(userName = "Fry", isAdmin = false)
        override val workspace: Workspace = Prototypes.workspace(owner = workspaceOwner)
        override val entities: MutableList<Any> = mutableListOf(workspaceOwner, workspace)

        override fun <T : Any> save(entity: T): T {
            entities.add(entity)
            return entity
        }
    }
}
