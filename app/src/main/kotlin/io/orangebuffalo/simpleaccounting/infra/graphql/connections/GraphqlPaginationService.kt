package io.orangebuffalo.simpleaccounting.infra.graphql.connections

import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import io.orangebuffalo.simpleaccounting.business.users.PlatformUsersService
import io.orangebuffalo.simpleaccounting.infra.withDbContext
import org.jooq.*
import org.jooq.impl.DSL
import org.jooq.impl.TableImpl
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.Base64

private val base64Encoder = Base64.getEncoder()
private val base64Decoder = Base64.getDecoder()
private const val CURSOR_DELIMITER = ":"

private fun encodeCursorFields(fields: List<String>): String =
    base64Encoder.encodeToString(fields.joinToString(CURSOR_DELIMITER).toByteArray())

private fun decodeCursorToFields(cursor: String): List<String> =
    String(base64Decoder.decode(cursor)).split(CURSOR_DELIMITER)

internal fun encodeCursor(createdAt: Instant): String =
    encodeCursorFields(listOf(createdAt.toEpochMilli().toString()))

data class PageSortSpec(
    val sortFields: List<SortField<*>>,
    val getCursorFields: (Record) -> List<String>,
    val buildCursorCondition: (List<String>) -> Condition,
)

@Service
class GraphqlPaginationService(
    private val dslContext: DSLContext,
    private val platformUsersService: PlatformUsersService,
) {
    fun <R : Record> forTable(table: TableImpl<R>): PaginationQueryBuilder<R> {
        return PaginationQueryBuilder(dslContext, platformUsersService, table)
    }
}

class PaginationQueryBuilder<R : Record>(
    private val dslContext: DSLContext,
    private val platformUsersService: PlatformUsersService,
    private val table: TableImpl<R>,
) {
    private val queryCustomizers = mutableListOf<(SelectJoinStep<*>) -> SelectJoinStep<*>>()
    private val predicates = mutableListOf<Condition>()

    @Suppress("UNCHECKED_CAST")
    private val createdAtField: Field<Instant> =
        (table.field("CREATED_AT") as? Field<Instant>)
            ?: error("Table ${table.name} does not have a CREATED_AT field")

    private val defaultSortSpec: PageSortSpec
        get() = PageSortSpec(
            sortFields = listOf(createdAtField.desc()),
            getCursorFields = { record -> listOf(record.get(createdAtField)!!.toEpochMilli().toString()) },
            buildCursorCondition = { parts -> createdAtField.lt(Instant.ofEpochMilli(parts[0].toLong())) },
        )

    fun onQuery(customizer: (SelectJoinStep<*>) -> SelectJoinStep<*>): PaginationQueryBuilder<R> {
        queryCustomizers.add(customizer)
        return this
    }

    fun addPredicate(condition: Condition): PaginationQueryBuilder<R> {
        predicates.add(condition)
        return this
    }

    suspend fun applyCurrentUserFiltering(
        predicateProvider: (PlatformUser) -> Condition,
    ): PaginationQueryBuilder<R> {
        val user = platformUsersService.getCurrentUser()
        predicates.add(predicateProvider(user))
        return this
    }

    suspend fun <N : Any> page(
        first: Int,
        after: String?,
        mapRecord: (Record) -> N,
    ): ConnectionGqlDto<N> = page(first, after, defaultSortSpec, mapQueryRecord = mapRecord, postProcess = { it })

    suspend fun <Q : Any, N : Any> page(
        first: Int,
        after: String?,
        mapQueryRecord: (Record) -> Q,
        postProcess: (List<Q>) -> List<N>,
    ): ConnectionGqlDto<N> = page(first, after, defaultSortSpec, mapQueryRecord = mapQueryRecord, postProcess = postProcess)

    suspend fun <N : Any> page(
        first: Int,
        after: String?,
        sortSpec: PageSortSpec,
        mapRecord: (Record) -> N,
    ): ConnectionGqlDto<N> = page(first, after, sortSpec, mapQueryRecord = mapRecord, postProcess = { it })

    suspend fun <Q : Any, N : Any> page(
        first: Int,
        after: String?,
        sortSpec: PageSortSpec,
        mapQueryRecord: (Record) -> Q,
        postProcess: (List<Q>) -> List<N>,
    ): ConnectionGqlDto<N> = withDbContext {
        val dataRecords = executeDataQuery(first, after, sortSpec)
        val totalCount = executeCountQuery()

        val hasNextPage = dataRecords.size > first
        val pageRecords = if (hasNextPage) dataRecords.dropLast(1) else dataRecords

        val mappedNodes = pageRecords.map(mapQueryRecord)
        val processedNodes = postProcess(mappedNodes)
        require(processedNodes.size == mappedNodes.size) {
            "postProcess must return the same number of items as the input"
        }

        val edges = pageRecords.zip(processedNodes).map { (record, node) ->
            EdgeGqlDto(
                cursor = encodeCursorFields(sortSpec.getCursorFields(record)),
                node = node,
            )
        }

        val startCursor = pageRecords.firstOrNull()?.let { encodeCursorFields(sortSpec.getCursorFields(it)) }
        val endCursor = pageRecords.lastOrNull()?.let { encodeCursorFields(sortSpec.getCursorFields(it)) }

        ConnectionGqlDto(
            edges = edges,
            pageInfo = PageInfoGqlDto(
                startCursor = startCursor,
                endCursor = endCursor,
                hasPreviousPage = after != null,
                hasNextPage = hasNextPage,
            ),
            totalCount = totalCount,
        )
    }

    private fun executeDataQuery(first: Int, after: String?, sortSpec: PageSortSpec): Result<out Record> {
        var query: SelectJoinStep<*> = dslContext
            .select(*table.fields())
            .from(table)

        for (customizer in queryCustomizers) {
            query = customizer(query)
        }

        var conditioned: SelectConditionStep<*> = query.where(predicates)

        if (after != null) {
            conditioned = conditioned.and(sortSpec.buildCursorCondition(decodeCursorToFields(after)))
        }

        return conditioned
            .orderBy(sortSpec.sortFields)
            .limit(first + 1)
            .fetch()
    }

    private fun executeCountQuery(): Int {
        var query: SelectJoinStep<*> = dslContext
            .select(DSL.count())
            .from(table)

        for (customizer in queryCustomizers) {
            query = customizer(query)
        }

        return query
            .where(predicates)
            .fetchOne(0, Int::class.java)!!
    }
}
