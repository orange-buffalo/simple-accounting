package io.orangebuffalo.simpleaccounting.infra.graphql.connections

import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import io.orangebuffalo.simpleaccounting.business.users.PlatformUsersService
import io.orangebuffalo.simpleaccounting.infra.withDbContext
import org.jooq.*
import org.jooq.impl.DSL
import org.jooq.impl.TableImpl
import org.springframework.stereotype.Service
import java.time.Instant

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
        mapRecord: (Record) -> N,
        postProcess: (List<N>) -> List<N> = { it },
    ): ConnectionGqlDto<N> = withDbContext {
        val cursorPage = decodeCursor(after)
        val dataRecords = executeDataQuery(first, cursorPage)
        val totalCount = executeCountQuery()

        val hasNextPage = dataRecords.size > first
        val pageRecords = if (hasNextPage) dataRecords.dropLast(1) else dataRecords

        val mappedNodes = pageRecords.map(mapRecord)
        val processedNodes = postProcess(mappedNodes)

        val edges = pageRecords.zip(processedNodes).map { (record, node) ->
            val createdAt = record.get(createdAtField)!!
            EdgeGqlDto(
                cursor = encodeCursor(createdAt),
                node = node,
            )
        }

        val startCursor = pageRecords.firstOrNull()?.let { encodeCursor(it.get(createdAtField)!!) }
        val endCursor = pageRecords.lastOrNull()?.let { encodeCursor(it.get(createdAtField)!!) }

        ConnectionGqlDto(
            edges = edges,
            pageInfo = PageInfoGqlDto(
                startCursor = startCursor,
                endCursor = endCursor,
                hasPreviousPage = cursorPage.createdAtAfter != null,
                hasNextPage = hasNextPage,
            ),
            totalCount = totalCount,
        )
    }

    private fun executeDataQuery(first: Int, cursorPage: CursorPage): Result<out Record> {
        var query: SelectJoinStep<*> = dslContext
            .select(*table.fields())
            .from(table)

        for (customizer in queryCustomizers) {
            query = customizer(query)
        }

        var conditioned: SelectConditionStep<*> = query.where(predicates)

        if (cursorPage.createdAtAfter != null) {
            conditioned = conditioned.and(createdAtField.gt(cursorPage.createdAtAfter))
        }

        return conditioned
            .orderBy(createdAtField.asc())
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
