package io.orangebuffalo.simpleaccounting.infra.graphql.connections

import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import io.orangebuffalo.simpleaccounting.business.users.PlatformUsersService
import io.orangebuffalo.simpleaccounting.infra.withDbContext
import org.jooq.*
import org.jooq.impl.DSL
import org.jooq.impl.TableImpl
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDate
import java.util.Base64

private val base64Encoder = Base64.getEncoder()
private val base64Decoder = Base64.getDecoder()
private const val CURSOR_DELIMITER = ":"

private fun encodeCursorParts(parts: List<String>): String =
    base64Encoder.encodeToString(parts.joinToString(CURSOR_DELIMITER).toByteArray())

private fun decodeCursorToParts(cursor: String): List<String> =
    String(base64Decoder.decode(cursor)).split(CURSOR_DELIMITER)

internal fun encodeCursor(createdAt: Instant): String =
    encodeCursorParts(listOf(createdAt.toEpochMilli().toString()))

private fun extractCursorPart(field: Field<*>, record: Record): String =
    when (field.type) {
        Instant::class.java -> (record.get(field) as Instant).toEpochMilli().toString()
        LocalDate::class.java -> (record.get(field) as LocalDate).toEpochDay().toString()
        else -> error("Unsupported cursor field type: ${field.type.simpleName} for field ${field.name}")
    }

@Suppress("UNCHECKED_CAST")
private fun buildCursorCondition(sortFields: List<SortField<*>>, cursorParts: List<String>): Condition {
    fun parseFieldValue(field: Field<*>, part: String): Any = when (field.type) {
        Instant::class.java -> Instant.ofEpochMilli(part.toLong())
        LocalDate::class.java -> LocalDate.ofEpochDay(part.toLong())
        else -> error("Unsupported cursor field type: ${field.type.simpleName} for field ${field.name}")
    }

    val conditions = sortFields.indices.map { i ->
        val sortField = sortFields[i]
        val field = sortField.`$field`()
        val cursorValue = parseFieldValue(field, cursorParts[i])
        val advanceCondition: Condition = if (sortField.`$sortOrder`() == SortOrder.DESC) {
            (field as Field<Any>).lt(cursorValue)
        } else {
            (field as Field<Any>).gt(cursorValue)
        }
        if (i == 0) {
            advanceCondition
        } else {
            val equalConditions = sortFields.take(i).mapIndexed { j, sf ->
                val f = sf.`$field`()
                (f as Field<Any>).eq(parseFieldValue(f, cursorParts[j]))
            }
            DSL.and(equalConditions + advanceCondition)
        }
    }
    return DSL.or(conditions)
}

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
        sortFields: List<SortField<*>> = listOf(createdAtField.desc()),
        mapRecord: (Record) -> N,
    ): ConnectionGqlDto<N> = page(first, after, sortFields, mapQueryRecord = mapRecord, postProcess = { it })

    suspend fun <Q : Any, N : Any> page(
        first: Int,
        after: String?,
        sortFields: List<SortField<*>> = listOf(createdAtField.desc()),
        mapQueryRecord: (Record) -> Q,
        postProcess: (List<Q>) -> List<N>,
    ): ConnectionGqlDto<N> = withDbContext {
        val dataRecords = executeDataQuery(first, after, sortFields)
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
                cursor = encodeCursorParts(sortFields.map { extractCursorPart(it.`$field`(), record) }),
                node = node,
            )
        }

        val startCursor = pageRecords.firstOrNull()
            ?.let { encodeCursorParts(sortFields.map { sf -> extractCursorPart(sf.`$field`(), it) }) }
        val endCursor = pageRecords.lastOrNull()
            ?.let { encodeCursorParts(sortFields.map { sf -> extractCursorPart(sf.`$field`(), it) }) }

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

    private fun executeDataQuery(first: Int, after: String?, sortFields: List<SortField<*>>): Result<out Record> {
        var query: SelectJoinStep<*> = dslContext
            .select(*table.fields())
            .from(table)

        for (customizer in queryCustomizers) {
            query = customizer(query)
        }

        var conditioned: SelectConditionStep<*> = query.where(predicates)

        if (after != null) {
            conditioned = conditioned.and(buildCursorCondition(sortFields, decodeCursorToParts(after)))
        }

        return conditioned
            .orderBy(sortFields)
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
