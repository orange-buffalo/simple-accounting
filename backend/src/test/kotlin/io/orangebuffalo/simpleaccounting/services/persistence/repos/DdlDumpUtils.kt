package io.orangebuffalo.simpleaccounting.services.persistence.repos

import org.springframework.jdbc.core.BeanPropertyRowMapper
import org.springframework.jdbc.core.JdbcTemplate

fun getDbDdl(jdbcTemplate: JdbcTemplate) = buildString {
    val columns = jdbcTemplate.query(
        """
            select *
            from information_schema.columns
            where table_schema = 'PUBLIC'
              and table_name != 'flyway_schema_history'
        """.trimIndent(), BeanPropertyRowMapper.newInstance(DbColumn::class.java)
    )

    val constraints = jdbcTemplate.query(
        """
            select *
            from information_schema.constraints
            where table_schema = 'PUBLIC'
              and table_name != 'flyway_schema_history'
        """.trimIndent(), BeanPropertyRowMapper.newInstance(DbConstraint::class.java)
    )

    val indexes = jdbcTemplate.query(
        """
            select *
            from information_schema.indexes
            where table_schema = 'PUBLIC'
              and table_name != 'flyway_schema_history'
              and is_generated = false
        """.trimIndent(), BeanPropertyRowMapper.newInstance(DbIndex::class.java)
    )

    columns.asSequence()
        .map { column -> column.tableName }
        .distinct()
        .sorted()
        .map { tableName -> generateSqlForTable(tableName, columns, constraints, indexes) }
        .forEach { tableSqlDefinitions -> appendln(tableSqlDefinitions) }
}

private fun generateSqlForTable(
    tableName: String,
    columns: MutableList<DbColumn>,
    constraints: MutableList<DbConstraint>,
    indexes: MutableList<DbIndex>
) = buildString {

    val primaryKeys = constraints.asSequence()
        .filter { constraint -> constraint.tableName == tableName }
        .filter { constraint -> constraint.isPrimaryKey }
        .map { constraint -> constraint.columnList }
        .flatMap { columnList -> columnList.splitToSequence(',') }
        .map { columnName -> columnName.trim() }
        .map { columnName -> columnName.toLowerCase() }
        .joinToString(", ")

    appendln("create table ${tableName.toLowerCase()} (")

    val columnsDefinitions = columns.asSequence()
        .filter { column -> column.tableName == tableName }
        .sortedBy { column -> column.columnName }
        .map { column -> column.createSqlDefinition() }
        .toList()

    columnsDefinitions.forEach { columnSqlDefinition ->
        appendln("$columnSqlDefinition,")
    }

    appendln()
    appendln("  primary key ($primaryKeys)")
    appendln(");")

    constraints.asSequence()
        .filter { constraint -> constraint.tableName == tableName }
        .filter { constraint -> !constraint.isPrimaryKey }
        .sortedBy { constraint -> constraint.constraintName }
        .map { constraint -> "${constraint.createSqlDefinition()};" }
        .forEach { constraintSqlDefinition -> appendln(constraintSqlDefinition) }

    indexes.asSequence()
        .filter { index -> index.tableName == tableName }
        .sortedBy { index -> index.indexName }
        .map { index -> "${index.createSqlDefinition()};" }
        .forEach { indexSqlDefinition -> appendln(indexSqlDefinition) }

    appendln()
}

data class DbColumn(
    var tableName: String = "",
    var columnName: String = "",
    var typeName: String = "",
    var characterMaximumLength: Int = 0,
    var isNullable: Boolean = true
) {
    fun createSqlDefinition(): String = buildString {
        val columnType = typeName.toLowerCase()
        append("  ${columnName.toLowerCase()} $columnType")

        if (columnType == "varchar") {
            append("($characterMaximumLength)")
        }

        if (!isNullable) {
            append(" not null")
        }
    }
}

data class DbConstraint(
    var tableName: String = "",
    var constraintName: String = "",
    var constraintType: String = "",
    var columnList: String = "",
    var sql: String = ""
) {
    fun createSqlDefinition(): String = sql.toLowerCase()
        // strip the schema name as we don't care
        .replace(""""public".""", "")
        // strip option we don't care about
        .replace(" nocheck", "")
        // remove indexes definitions from constraints
        .replace(""" index [^\s]+""".toRegex(), "")
        // de-quote database entities name
        .replace(""""([^\s^"]+)"""".toRegex()) { matcherResult ->
            matcherResult.groupValues[1]
        }

    val isPrimaryKey: Boolean
        get() = constraintType.equals("primary key", ignoreCase = true)
}

data class DbIndex(
    var tableName: String = "",
    var indexName: String = "",
    var sql: String = ""
) {
    fun createSqlDefinition(): String = sql.toLowerCase()
        // strip the schema name as we don't care
        .replace(""""public".""", "")
        // de-quote database entities name
        .replace(""""([^\s^"]+)"""".toRegex()) { matcherResult ->
            matcherResult.groupValues[1]
        }
}

