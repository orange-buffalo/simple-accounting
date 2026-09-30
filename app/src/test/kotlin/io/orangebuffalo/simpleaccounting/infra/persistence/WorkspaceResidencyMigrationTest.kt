package io.orangebuffalo.simpleaccounting.infra.persistence

import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import javax.sql.DataSource

class WorkspaceResidencyMigrationTest(
    @Autowired private val dataSource: DataSource,
) : SaIntegrationTestBase() {
    @Test
    fun `should backfill Australia for existing workspaces without changing their currencies`() {
        val schema = "residency_${UUID.randomUUID().toString().replace("-", "")}"
        try {
            Flyway.configure().dataSource(dataSource).schemas(schema)
                .locations("classpath:db/migration").target("0001").load().migrate()
            dataSource.connection.use { connection ->
                connection.createStatement().use { statement ->
                    statement.executeUpdate("""
                        insert into $schema.platform_user
                            (id, version, user_name, password_hash, is_admin, failed_attempts_count, language, locale, activated, created_at)
                        values ('fry', 0, 'Fry', 'hashed', false, 0, 'en', 'en', true, timestamp '1999-03-28 23:01:02')
                    """.trimIndent())
                    statement.executeUpdate("""
                        insert into $schema.workspace (id, version, name, owner_id, default_currency, created_at)
                        values ('planet', 0, 'Planet Express', 'fry', 'USD', timestamp '1999-03-28 23:01:02'),
                               ('slurm', 0, 'Slurm Corp', 'fry', 'EUR', timestamp '1999-03-28 23:01:02')
                    """.trimIndent())
                }
            }
            Flyway.configure().dataSource(dataSource).schemas(schema)
                .locations("classpath:db/migration").load().migrate()
            dataSource.connection.use { connection ->
                connection.createStatement().use { statement ->
                    statement.executeQuery("select residency, default_currency from $schema.workspace order by id").use { rows ->
                        rows.next().shouldBe(true)
                        rows.getString("residency").shouldBe("AU")
                        rows.getString("default_currency").shouldBe("USD")
                        rows.next().shouldBe(true)
                        rows.getString("residency").shouldBe("AU")
                        rows.getString("default_currency").shouldBe("EUR")
                        rows.next().shouldBe(false)
                    }
                }
            }
        } finally {
            dataSource.connection.use { connection ->
                connection.createStatement().use { it.execute("drop schema if exists $schema cascade") }
            }
        }
    }
}
