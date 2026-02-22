package io.orangebuffalo.simpleaccounting.infra.rest.filtering

import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.jooq.Field
import org.jooq.Record2
import org.jooq.Row2
import org.jooq.TableField
import org.jooq.impl.DSL
import org.jooq.impl.TableImpl
import org.jooq.impl.UpdatableRecordImpl
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.relational.core.mapping.Table
import org.springframework.jdbc.core.JdbcTemplate
import java.util.stream.Stream

internal class FilteringApiExecutorTest(
    @Autowired val jdbcTemplate: JdbcTemplate,
    @Autowired val filteringApiExecutorBuilder: FilteringApiExecutorBuilder
) : SaIntegrationTestBase() {
    lateinit var filteringApiExecutor: FilteringApiExecutor<TestDummy, TestDummyDto, TestDummySortFields, TestDummyApiPageRequest>

    companion object {
        private val zoidberg = TestDummyDto(2, "Zoidberg")
        private val fry = TestDummyDto(4, "Fry")
        private val leela = TestDummyDto(10, "Leela")
        private val bender = TestDummyDto(19, "Bender")

        @Suppress("unused")
        @JvmStatic
        fun getUseCases(): Stream<FilteringApiQueryExecutorUseCase> = Stream.of(
            FilteringApiQueryExecutorUseCase(
                description = "should return all records in default order when no preferences provided",
                request = TestDummyApiPageRequest().apply {
                    pageNumber = 1
                    pageSize = 10
                },
                expectedTotalRecordsCount = 4,
                expectedRecords = listOf(zoidberg, fry, leela, bender)
            ),

            FilteringApiQueryExecutorUseCase(
                description = "should apply pagination",
                request = TestDummyApiPageRequest().apply {
                    pageSize = 1
                    pageNumber = 3
                },
                expectedTotalRecordsCount = 4,
                expectedRecords = listOf(leela)
            ),

            FilteringApiQueryExecutorUseCase(
                description = "should apply pagination and return no results if page has too high value",
                request = TestDummyApiPageRequest().apply {
                    pageNumber = 3
                },
                expectedTotalRecordsCount = 4,
                expectedRecords = listOf()
            ),

            FilteringApiQueryExecutorUseCase(
                description = "should filter by string value",
                request = TestDummyApiPageRequest().apply {
                    searchEq = leela.name
                },
                expectedTotalRecordsCount = 1,
                expectedRecords = listOf(leela)
            ),

            FilteringApiQueryExecutorUseCase(
                description = "should filter by long value",
                request = TestDummyApiPageRequest().apply {
                    idEq = bender.id
                },
                expectedTotalRecordsCount = 1,
                expectedRecords = listOf(bender)
            ),

            FilteringApiQueryExecutorUseCase(
                description = "should filter by long values",
                request = TestDummyApiPageRequest().apply {
                    idIn = listOf(bender.id, leela.id)
                },
                expectedTotalRecordsCount = 2,
                expectedRecords = listOf(leela, bender)
            ),
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

        filteringApiExecutor = filteringApiExecutorBuilder.executor {
            query(TestDummyTable.testDummy) {
                onFilter(TestDummyApiPageRequest::searchEq) { value ->
                    root.name.eq(value)
                }
                onFilter(TestDummyApiPageRequest::idEq) { id ->
                    root.id.eq(id)
                }
                onFilter(TestDummyApiPageRequest::idIn) { ids ->
                    root.id.`in`(ids)
                }
                addDefaultSorting { root.id.asc() }
            }
            mapper { TestDummyDto(id, name) }
        }
    }

    @ParameterizedTest
    @MethodSource("getUseCases")
    fun testExecuteFilteringQuery(useCase: FilteringApiQueryExecutorUseCase) {
        val page = runBlocking { filteringApiExecutor.executeFiltering(useCase.request) }
        assertThat(page.pageNumber).isEqualTo(useCase.request.pageNumber ?: 1)
        assertThat(page.pageSize).isEqualTo(useCase.request.pageSize ?: 10)
        assertThat(page.totalElements).isEqualTo(useCase.expectedTotalRecordsCount)
        assertThat(page.data).containsExactly(*useCase.expectedRecords.toTypedArray())
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

    data class TestDummyDto(
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

    class TestDummyApiPageRequest : ApiPageRequest<TestDummySortFields>() {
        override var sortBy: TestDummySortFields? = null
        var searchEq: String? = null
        var idEq: Long? = null
        var idIn: List<Long>? = null
    }

    @Suppress("EnumEntryName")
    enum class TestDummySortFields {
        id
    }

    data class FilteringApiQueryExecutorUseCase(
        val description: String,
        val request: TestDummyApiPageRequest,
        val expectedRecords: List<TestDummyDto>,
        val expectedTotalRecordsCount: Long,
    ) {
        override fun toString() = description
    }
}

