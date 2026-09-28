package io.orangebuffalo.simpleaccounting.infra.persistence

import io.orangebuffalo.simpleaccounting.infra.DatabaseProperties
import mu.KotlinLogging
import org.flywaydb.core.Flyway
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Types
import javax.sql.DataSource

private val logger = KotlinLogging.logger {}

@Component
class LegacyH2Import(
    private val dataSource: DataSource,
    private val databaseProperties: DatabaseProperties,
) : ApplicationRunner {

    override fun run(args: ApplicationArguments) = importFrom(databaseProperties.legacyH2Path)

    fun importFrom(sourcePath: Path) {
        val sourceFile = sourcePath.resolveSibling("${sourcePath.fileName}.mv.db")
        if (!Files.isRegularFile(sourceFile)) {
            logger.info { "No H2 database at $sourceFile; skipping import" }
            return
        }

        dataSource.connection.use { target ->
            target.autoCommit = false
            try {
                target.createStatement().use { it.execute("lock table sa_h2_import in exclusive mode") }
                if (count(target, "sa_h2_import") != 0L) {
                    target.commit()
                    return
                }

                val tables = tables(target)
                require(tables.all { count(target, it) == 0L }) {
                    "PostgreSQL already contains application data; refusing to import $sourceFile"
                }

                val temporaryDirectory = Files.createTempDirectory("sa-h2-import-")
                try {
                    val temporaryDatabase = temporaryDirectory.resolve("source")
                    Files.copy(sourceFile, temporaryDirectory.resolve("source.mv.db"))
                    val sourceUrl = "jdbc:h2:file:$temporaryDatabase;IFEXISTS=TRUE"
                    val sourceUsername = databaseProperties.legacyH2Username ?: databaseProperties.username
                    val sourcePassword = databaseProperties.legacyH2Password ?: databaseProperties.password
                    val sourceFlyway = Flyway.configure()
                        .dataSource(sourceUrl, sourceUsername, sourcePassword)
                        .locations("classpath:db/h2")
                        .baselineOnMigrate(true)
                        .baselineVersion("0001")
                        .load()
                    sourceFlyway.migrate()

                    DriverManager.getConnection(sourceUrl, sourceUsername, sourcePassword).use { source ->
                        val sourceTables = tables(source)
                        require(sourceTables == tables) {
                            "The H2 and PostgreSQL application tables differ: H2=$sourceTables, PostgreSQL=$tables"
                        }
                        copyOrder(target, tables).forEach { table -> copyTable(source, target, table) }
                    }
                } finally {
                    Files.deleteIfExists(temporaryDirectory.resolve("source.mv.db"))
                    Files.deleteIfExists(temporaryDirectory.resolve("source.trace.db"))
                    Files.delete(temporaryDirectory)
                }

                target.createStatement().use {
                    it.executeUpdate("insert into sa_h2_import (completed_at) values (current_timestamp)")
                }
                target.commit()
                logger.info { "Imported legacy H2 data from $sourceFile into PostgreSQL" }
            } catch (error: Exception) {
                target.rollback()
                throw error
            }
        }
    }

    private fun tables(connection: Connection): Set<String> {
        val schema = if (connection.metaData.databaseProductName == "PostgreSQL") "public" else "PUBLIC"
        val excluded = setOf("flyway_schema_history", "sa_h2_import")
        return connection.metaData.getTables(null, schema, "%", arrayOf("TABLE")).use { result ->
            buildSet {
                while (result.next()) {
                    result.getString("TABLE_NAME").lowercase().takeUnless { it in excluded }?.let(::add)
                }
            }
        }
    }

    private fun copyOrder(target: Connection, tables: Set<String>): List<String> {
        val dependencies = tables.associateWith { table ->
            target.metaData.getImportedKeys(null, "public", table).use { keys ->
                buildSet {
                    while (keys.next()) add(keys.getString("PKTABLE_NAME"))
                }
            }
        }
        require(dependencies.values.all { it.all(tables::contains) }) {
            "PostgreSQL application tables reference tables outside the import: $dependencies"
        }

        val remaining = tables.toMutableSet()
        val order = mutableListOf<String>()
        while (remaining.isNotEmpty()) {
            val ready = remaining.filter { table -> dependencies.getValue(table).all(order::contains) }.sorted()
            require(ready.isNotEmpty()) { "Cyclic foreign keys prevent importing tables: $remaining" }
            order.addAll(ready)
            remaining.removeAll(ready.toSet())
        }
        return order
    }

    private fun copyTable(source: Connection, target: Connection, table: String) {
        val sourceTable = "\"${table.uppercase()}\""
        source.createStatement().use { statement ->
            statement.executeQuery("select * from $sourceTable").use { rows ->
                val columns = (1..rows.metaData.columnCount).map { rows.metaData.getColumnName(it).lowercase() }
                val targetColumns = target.metaData.getColumns(null, "public", table, "%").use { metadata ->
                    buildSet {
                        while (metadata.next()) add(metadata.getString("COLUMN_NAME"))
                    }
                }
                require(columns.toSet() == targetColumns) {
                    "Columns differ for $table: H2=$columns, PostgreSQL=$targetColumns"
                }

                val sql = "insert into $table (${columns.joinToString()}) values (${columns.joinToString { "?" }})"
                target.prepareStatement(sql).use { insert ->
                    var copied = 0L
                    while (rows.next()) {
                        for (index in columns.indices) {
                            val column = index + 1
                            val value = rows.getObject(column)
                            if (value == null) insert.setNull(column, rows.metaData.getColumnType(column).takeUnless { it == Types.NULL } ?: Types.VARCHAR)
                            else insert.setObject(column, value)
                        }
                        insert.addBatch()
                        copied++
                        if (copied % 500 == 0L) insert.executeBatch()
                    }
                    insert.executeBatch()
                    check(count(target, table) == copied) { "Row count mismatch while importing $table" }
                }
            }
        }
    }

    private fun count(connection: Connection, table: String): Long =
        connection.createStatement().use { statement ->
            statement.executeQuery("select count(*) from $table").use { rows ->
                rows.next()
                rows.getLong(1)
            }
        }
}
