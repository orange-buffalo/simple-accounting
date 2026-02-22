package io.orangebuffalo.simpleaccounting.infra.rest.filtering

import io.orangebuffalo.simpleaccounting.infra.rest.errorhandling.ApiValidationException
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.jooq.*
import org.jooq.impl.DSL
import org.jooq.impl.TableImpl
import org.jooq.impl.UpdatableRecordImpl
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.convert.ConversionService
import org.springframework.data.relational.core.mapping.Table
import org.springframework.jdbc.core.JdbcTemplate
import reactor.core.publisher.Mono
import java.util.stream.Stream

internal class FilteringApiQueryExecutorLegacyTest(
    @Autowired val jdbcTemplate: JdbcTemplate,
    @Autowired val dslContext: DSLContext,
    @Autowired val conversionService: ConversionService
) : SaIntegrationTestBase() {
    lateinit var filteringApiQueryExecutor: FilteringApiQueryExecutorLegacy<TestDummyTable, TestDummy>

    companion object {
        private val defaultRequest = FilteringApiRequest(
            pageNumber = 1,
            pageSize = 10,
            sortBy = null,
            sortDirection = null,
            predicates = emptyList()
        )
        private val zoidberg = TestDummy(2, "Zoidberg")
        private val fry = TestDummy(4, "Fry")
        private val leela = TestDummy(10, "Leela")
        private val bender = TestDummy(19, "Bender")

        @Suppress("unused")
        @JvmStatic
        fun getUseCases(): Stream<FilteringApiQueryExecutorUseCase> = Stream.of(
            FilteringApiQueryExecutorUseCase(
                description = "should return all records in default order when no preferences provided",
                request = defaultRequest,
                expectedTotalRecordsCount = 4,
                expectedRecords = listOf(zoidberg, fry, leela, bender)
            ),

            FilteringApiQueryExecutorUseCase(
                description = "should apply pagination",
                request = defaultRequest.copy(
                    pageSize = 1,
                    pageNumber = 3
                ),
                expectedTotalRecordsCount = 4,
                expectedRecords = listOf(leela)
            ),

            FilteringApiQueryExecutorUseCase(
                description = "should apply pagination and return no results if page has too high value",
                request = defaultRequest.copy(pageNumber = 3),
                expectedTotalRecordsCount = 4,
                expectedRecords = listOf()
            ),

            FilteringApiQueryExecutorUseCase(
                description = "should fail if filtering field is not known",
                request = defaultRequest.copy(
                    predicates = listOf(
                        FilteringApiRequestPredicate(
                            apiField = "unknown",
                            value = "42",
                            operator = FilteringApiPredicateOperator.EQ
                        )
                    )
                ),
                expectedError = "Filtering by 'unknown' is not supported"
            ),

            FilteringApiQueryExecutorUseCase(
                description = "should fail if operator is not supported for field",
                request = defaultRequest.copy(
                    predicates = listOf(
                        FilteringApiRequestPredicate(
                            apiField = "search",
                            value = "42",
                            operator = FilteringApiPredicateOperator.IN
                        )
                    )
                ),
                expectedError = "'in' operator is not supported for 'search' filter"
            ),

            FilteringApiQueryExecutorUseCase(
                description = "should filter by string value",
                request = defaultRequest.copy(
                    predicates = listOf(
                        FilteringApiRequestPredicate(
                            apiField = "search",
                            value = leela.name,
                            operator = FilteringApiPredicateOperator.EQ
                        )
                    )
                ),
                expectedTotalRecordsCount = 1,
                expectedRecords = listOf(leela)
            ),

            FilteringApiQueryExecutorUseCase(
                description = "should filter by long value",
                request = defaultRequest.copy(
                    predicates = listOf(
                        FilteringApiRequestPredicate(
                            apiField = "id",
                            value = "${bender.id}",
                            operator = FilteringApiPredicateOperator.EQ
                        )
                    )
                ),
                expectedTotalRecordsCount = 1,
                expectedRecords = listOf(bender)
            ),

            FilteringApiQueryExecutorUseCase(
                description = "should fail filtering by long value if value has bad format",
                request = defaultRequest.copy(
                    predicates = listOf(
                        FilteringApiRequestPredicate(
                            apiField = "id",
                            value = "42#",
                            operator = FilteringApiPredicateOperator.EQ
                        )
                    )
                ),
                expectedError = "Cannot convert '42#' to Long ('id')"
            ),

            FilteringApiQueryExecutorUseCase(
                description = "should filter by long values",
                request = defaultRequest.copy(
                    predicates = listOf(
                        FilteringApiRequestPredicate(
                            apiField = "id",
                            value = "${bender.id},${leela.id}",
                            operator = FilteringApiPredicateOperator.IN
                        )
                    )
                ),
                expectedTotalRecordsCount = 2,
                expectedRecords = listOf(leela, bender)
            ),

            FilteringApiQueryExecutorUseCase(
                description = "should fail filtering by unsupported",
                request = defaultRequest.copy(
                    predicates = listOf(
                        FilteringApiRequestPredicate(
                            apiField = "unsupportedType",
                            value = "42",
                            operator = FilteringApiPredicateOperator.EQ
                        )
                    )
                ),
                expectedError = "Cannot convert '42' to Mono ('unsupportedType')"
            )
        )
    }

    @Suppress("SqlResolve")
    @BeforeEach
    fun setup() {
        jdbcTemplate.execute(
            """
            create table if not exists filtering_api_query_dummy(
                id bigint not null,
                name varchar(255) not null
            )
        """
        )

        jdbcTemplate.execute("delete from filtering_api_query_dummy")
        jdbcTemplate.execute("insert into filtering_api_query_dummy values(${leela.id}, '${leela.name}')")
        jdbcTemplate.execute("insert into filtering_api_query_dummy values(${fry.id}, '${fry.name}')")
        jdbcTemplate.execute("insert into filtering_api_query_dummy values(${bender.id}, '${bender.name}')")
        jdbcTemplate.execute("insert into filtering_api_query_dummy values(${zoidberg.id}, '${zoidberg.name}')")

        filteringApiQueryExecutor = FilteringApiQueryExecutorLegacy(
            dslContext = dslContext,
            conversionService = conversionService,
            root = TestDummyTable.testDummy,
            entityType = TestDummy::class,
            init = {
                filterByField("search", String::class) {
                    onPredicate(FilteringApiPredicateOperator.EQ) { value ->
                        root.name.eq(value)
                    }
                }
                filterByField("id", Long::class) {
                    onPredicate(FilteringApiPredicateOperator.EQ) { id ->
                        root.id.eq(id)
                    }
                    onPredicate(FilteringApiPredicateOperator.IN) { ids ->
                        root.id.`in`(ids)
                    }
                }
                filterByField("unsupportedType", Mono::class) {
                    onPredicate(FilteringApiPredicateOperator.EQ) { DSL.trueCondition() }
                }
                addDefaultSorting { root.id.asc() }
            }
        )
    }

    @ParameterizedTest
    @MethodSource("getUseCases")
    fun testExecuteFilteringQuery(useCase: FilteringApiQueryExecutorUseCase) {
        when {
            useCase.expectedError != null -> executeErrorTest(useCase)
            useCase.expectedRecords != null && useCase.expectedTotalRecordsCount != null -> executeSuccessTest(useCase)
            else -> throw IllegalArgumentException("$useCase is not supported")
        }
    }

    private fun executeSuccessTest(useCase: FilteringApiQueryExecutorUseCase) {
        val page = runBlocking { filteringApiQueryExecutor.executeFilteringQuery(useCase.request) }
        assertThat(page.pageNumber).isEqualTo(useCase.request.pageNumber)
        assertThat(page.pageSize).isEqualTo(useCase.request.pageSize)
        assertThat(page.totalElements).isEqualTo(useCase.expectedTotalRecordsCount)
        assertThat(page.data).containsExactly(*useCase.expectedRecords!!.toTypedArray())
    }

    private fun executeErrorTest(useCase: FilteringApiQueryExecutorUseCase) {
        assertThatThrownBy {
            runBlocking { filteringApiQueryExecutor.executeFilteringQuery(useCase.request) }
        }.isInstanceOf(ApiValidationException::class.java).hasMessage(useCase.expectedError)
    }

    class TestDummyTable : TableImpl<TestDummyRecord>(DSL.name("FILTERING_API_QUERY_DUMMY")) {
        companion object {
            val testDummy = TestDummyTable()
        }

        val id: TableField<TestDummyRecord, Long> =
            createField(DSL.name("ID"), org.jooq.impl.SQLDataType.BIGINT.nullable(false), this, "")
        val name: TableField<TestDummyRecord, String> =
            createField(DSL.name("NAME"), org.jooq.impl.SQLDataType.VARCHAR(256), this, "")

        override fun getRecordType(): Class<out TestDummyRecord> = TestDummyRecord::class.java
    }

    @Table
    data class TestDummy(
        var id: Long,
        var name: String
    )

    @Suppress("UNCHECKED_CAST")
    class TestDummyRecord : UpdatableRecordImpl<TestDummyRecord>(TestDummyTable.testDummy), Record2<Long, String> {
        var id: Long
            get() = get(0) as Long
            set(value) = set(0, value)

        var name: String
            get() = get(1) as String
            set(value) = set(1, value)

        override fun fieldsRow(): Row2<Long, String> = super.fieldsRow() as Row2<Long, String>
        override fun valuesRow(): Row2<Long, String> = super.valuesRow() as Row2<Long, String>
        override fun field1(): Field<Long> = TestDummyTable.testDummy.id
        override fun field2(): Field<String> = TestDummyTable.testDummy.name
        override fun value1(): Long = id
        override fun value2(): String = name
        override fun component1(): Long = id
        override fun component2(): String = name

        override fun values(t1: Long, t2: String): TestDummyRecord {
            value1(t1)
            value2(t2)
            return this
        }

        override fun value1(value: Long): TestDummyRecord {
            id = value
            return this
        }

        override fun value2(value: String): TestDummyRecord {
            name = value
            return this
        }

        override fun toString(): String = "[$name ($id)]"
    }

    data class FilteringApiQueryExecutorUseCase(
        val description: String,
        val request: FilteringApiRequest,
        val expectedRecords: List<TestDummy>? = null,
        val expectedTotalRecordsCount: Long? = null,
        val expectedError: String? = null
    ) {
        override fun toString() = description
    }
}

