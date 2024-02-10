package io.orangebuffalo.simpleaccounting.web

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsOnly
import assertk.assertions.isTrue
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import io.orangebuffalo.simpleaccounting.infra.api.verifyOkAndBody
import io.orangebuffalo.simpleaccounting.infra.database.Prototypes
import io.orangebuffalo.simpleaccounting.infra.security.asFarnsworth
import io.orangebuffalo.simpleaccounting.infra.security.asFry
import io.orangebuffalo.simpleaccounting.infra.utils.combine
import io.orangebuffalo.simpleaccounting.infra.utils.logger
import io.orangebuffalo.simpleaccounting.services.persistence.entities.PlatformUser
import io.orangebuffalo.simpleaccounting.services.persistence.entities.Workspace
import io.orangebuffalo.simpleaccounting.support.kotlinEquals
import io.orangebuffalo.simpleaccounting.support.kotlinHashCode
import org.springframework.test.web.reactive.server.WebTestClient
import kotlin.math.max
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

class FilteringApiTestCasesBuilderImpl<T : Any>(
    entityType: KClass<T>,
) : FilteringApiTestCasesBuilder<T> {

    override lateinit var baseUrl: String
    override var workspaceBasedUrl: Boolean = true
    override var executeAsAdmin: Boolean = false

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
        if (filteringBuilder.entitiesSpecs.isNotEmpty()) {
            testCases.add(FilteringTestCase())
        }
        addStandaloneFiltersTestCases(testCases)
        addFiltersCombinationsTestCases(testCases)

        if (sortingBuilder.defaultCase.entitiesInitiators.isNotEmpty()) {
            testCases.add(SortingTestCase(sortingBuilder.defaultCase.entitiesInitiators))
        }
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
        val filtersPerField = filteringBuilder.allFilters
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
        val dynamicFilterReplacements: MutableList<DynamicFilterReplacement> = mutableListOf()

        override fun configure(init: EntitiesRegistry.(entity: T) -> Unit) {
            this.entityConfigurer = init
        }

        override fun skippedOn(filter: String) {
            skippedOnFilters.add(filter)
        }

        override fun dynamicFilterReplacement(placeholder: String, valueExtractor: (T) -> Any?) {
            dynamicFilterReplacements.add(
                this@FilteringApiTestCasesBuilderImpl.DynamicFilterReplacement(placeholder, valueExtractor)
            )
        }

        fun createEntity(entitiesRegistry: EntitiesRegistry): T {
            val entity = this@FilteringApiTestCasesBuilderImpl.entityInitiator(entitiesRegistry)
            entityConfigurer(entitiesRegistry, entity)
            entitiesRegistry.save(entity)
            return entity
        }

        fun isSkippedForUrl(url: String): Boolean = skippedOnFilters.any { url.contains(it) }

        fun createFilterReplacements(entity: T): List<(String) -> String> {
            return dynamicFilterReplacements
                .map { filterReplacementSpec ->
                    { url: String ->
                        val placeholderValue = filterReplacementSpec.valueExtractor(entity)?.toString() ?: ""
                        url.replace(filterReplacementSpec.placeholder, placeholderValue, true)
                    }
                }
        }
    }

    private inner class DynamicFilterReplacement(
        val placeholder: String,
        val valueExtractor: (T) -> Any?
    )

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

    private inner class FilteringTestCase : AbstractFilteringApiTestCase(
        initialUrl = baseUrl,
        workspaceBasedUrl = workspaceBasedUrl,
        executeAsAdmin = executeAsAdmin,
    ) {

        private val filters: MutableList<String> = mutableListOf()
        private val equalsHashCodeProperties: Array<out (obj: FilteringTestCase) -> Any?>
            get() = arrayOf({ tc -> tc.filters })
        private val filterReplacements = mutableListOf<(String) -> String>()

        init {
            val pageSize = max(1, filteringBuilder.entitiesSpecs.size)
            // todo remove limit once we migrate all APIs
            url += "?pageSize=$pageSize&limit=${pageSize}"
        }

        fun addQueryParam(filter: String) {
            url += "&$filter"
            filters.add(filter)
        }

        override fun generateData(): List<Any> {
            filteringBuilder.entitiesSpecs.forEach { entitySpec ->
                val entity = entitySpec.createEntity(entitiesRegistry)
                if (!entitySpec.isSkippedForUrl(url)) {
                    expectedEntitiesInResponse.add(entity)
                }
                filterReplacements.addAll(entitySpec.createFilterReplacements(entity))
            }

            return super.generateData()
        }

        override fun execute(client: WebTestClient) {
            filterReplacements.forEach { filterReplacementSpec -> url = filterReplacementSpec(url) }
            super.execute(client)
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
    ) : AbstractFilteringApiTestCase(
        initialUrl = baseUrl,
        workspaceBasedUrl = workspaceBasedUrl,
        executeAsAdmin = executeAsAdmin,
    ) {

        init {
            url += (if (sortingQueryParam == null) "?" else "?$sortingQueryParam&") +
                    // todo remove limit once all APIs are migrated
                    "limit=${entitiesInitiators.size}&pageSize=${entitiesInitiators.size}"
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
        initialUrl: String,
        private val workspaceBasedUrl: Boolean,
        private val executeAsAdmin: Boolean,
    ) : FilteringApiTestCase() {

        var url: String = initialUrl
        val entitiesRegistry: EntitiesRegistry = EntitiesRegistryImpl(skipWorkspaceEntities = !workspaceBasedUrl)
        val expectedEntitiesInResponse: MutableList<T> = mutableListOf()

        override fun generateData() = entitiesRegistry.entities

        override fun toString() = if (workspaceBasedUrl) "/api/workspaces/{workspace}/$url" else "/api/$url"

        abstract fun assertResults(responseEntitiesDesc: List<String>, expectedEntitiesDesc: List<String>)

        override fun execute(client: WebTestClient) {
            logger.info { "Executing $this" }
            val requestUrl = if (workspaceBasedUrl) {
                "/api/workspaces/${entitiesRegistry.workspace.id}/$url"
            } else {
                "/api/$url"
            }
            client
                .let { if (executeAsAdmin) it.asFarnsworth() else it.asFry() }
                .get()
                .uri(requestUrl)
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

    private class EntitiesRegistryImpl(skipWorkspaceEntities: Boolean) : EntitiesRegistry {

        override val workspaceOwner: PlatformUser = Prototypes.platformUser(userName = "Fry", isAdmin = false)
        override val workspace: Workspace = Prototypes.workspace(owner = workspaceOwner)
        override val entities: MutableList<Any> = if (skipWorkspaceEntities)
            mutableListOf()
        else
            mutableListOf(workspaceOwner, workspace)

        override fun <T : Any> save(entity: T): T {
            entities.add(entity)
            return entity
        }
    }
}
