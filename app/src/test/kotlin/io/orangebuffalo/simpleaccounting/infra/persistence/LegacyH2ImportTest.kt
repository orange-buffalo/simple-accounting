package io.orangebuffalo.simpleaccounting.infra.persistence

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.MigrationVersion
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import javax.sql.DataSource

@TestPropertySource(properties = [
    "sa.database.legacy-h2-username=sa",
    "sa.database.legacy-h2-password=killallhumans",
])
class LegacyH2ImportTest(
    @Autowired private val importer: LegacyH2Import,
    @Autowired private val dataSource: DataSource,
) : SaIntegrationTestBase() {

    @TempDir
    lateinit var directory: Path

    @Test
    fun `imports all application entities and their references without changing the H2 file`() {
        val source = createH2Database()
        connectToH2(source).use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate("""insert into "PLATFORM_USER" ("ID", "VERSION", "USER_NAME", "PASSWORD_HASH", "IS_ADMIN", "FAILED_ATTEMPTS_COUNT", "LANGUAGE", "LOCALE", "ACTIVATED", "CREATED_AT") values ('fry', 0, 'Fry', 'hashed', true, 0, 'en', 'en', true, timestamp '1999-03-28 23:01:02')""")
                statement.executeUpdate("""insert into "PERSISTENT_OAUTH2_AUTHORIZED_CLIENT" ("ID", "VERSION", "ACCESS_TOKEN", "CLIENT_REGISTRATION_ID", "USER_NAME", "CREATED_AT") values ('client1', 0, 'slurm-access', 'planet-express', 'Fry', timestamp '1999-03-28 23:01:02')""")
                statement.executeUpdate("""insert into "PERSISTENT_OAUTH2_AUTHORIZED_CLIENT_ACCESS_TOKEN_SCOPES" ("CLIENT_ID", "ACCESS_TOKEN_SCOPES") values ('client1', 'openid')""")
                statement.executeUpdate("""insert into "OAUTH_PROVIDER" ("ID", "VERSION", "CREATED_AT", "NAME", "CLIENT_ID", "CLIENT_SECRET", "AUTHORIZATION_URL", "TOKEN_URL", "USER_INFO_URL", "USER_ID_ATTRIBUTE") values ('provider1', 0, timestamp '1999-03-28 23:01:02', 'Planet Express SSO', 'px-client', 'px-secret', 'https://planet.express/authorize', 'https://planet.express/token', 'https://planet.express/user', 'sub')""")
                statement.executeUpdate("""insert into "OAUTH_PROVIDER_SCOPE" ("PROVIDER_ID", "SCOPE") values ('provider1', 'openid')""")
                statement.executeUpdate("""insert into "WORKSPACE" ("ID", "VERSION", "NAME", "OWNER_ID", "DEFAULT_CURRENCY", "CREATED_AT") values ('planet', 0, 'Planet Express', 'fry', 'USD', timestamp '1999-03-28 23:01:02')""")
                statement.executeUpdate("""insert into "REFRESH_TOKEN" ("ID", "VERSION", "EXPIRATION_TIME", "TOKEN", "USER_ID", "CREATED_AT") values ('refresh1', 0, timestamp '3025-01-15 00:00:00', 'slurm-refresh', 'fry', timestamp '1999-03-28 23:01:02')""")
                statement.executeUpdate("""insert into "GOOGLE_DRIVE_STORAGE_INTEGRATION" ("ID", "VERSION", "FOLDER_ID", "USER_ID", "CREATED_AT") values ('drive1', 0, 'mars-deliveries', 'fry', timestamp '1999-03-28 23:01:02')""")
                statement.executeUpdate("""insert into "USER_ACTIVATION_TOKEN" ("ID", "VERSION", "USER_ID", "TOKEN", "EXPIRES_AT", "CREATED_AT") values ('activate1', 0, 'fry', 'activate-fry', timestamp '3025-01-15 00:00:00', timestamp '1999-03-28 23:01:02')""")
                statement.executeUpdate("""insert into "DOCUMENTS_MIGRATION" ("ID", "VERSION", "CREATED_AT", "USER_ID", "MIGRATED_DOCUMENTS_COUNT") values ('migration1', 0, timestamp '1999-03-28 23:01:02', 'fry', 1)""")
                statement.executeUpdate("""insert into "USER_OAUTH_IDENTITY" ("ID", "VERSION", "CREATED_AT", "USER_ID", "PROVIDER_ID", "EXTERNAL_ID") values ('identity1', 0, timestamp '1999-03-28 23:01:02', 'fry', 'provider1', 'fry-at-planet-express')""")
                statement.executeUpdate("""insert into "OAUTH_AUTHENTICATION_REQUEST" ("ID", "VERSION", "CREATED_AT", "STATE", "BROWSER_BINDING", "PROVIDER_ID", "USER_ID", "PURPOSE", "ISSUE_REFRESH_TOKEN_COOKIE", "EXPIRES_AT") values ('request1', 0, timestamp '1999-03-28 23:01:02', 'state-fry', 'browser-fry', 'provider1', 'fry', 'LOGIN', true, timestamp '3025-01-15 00:00:00')""")
                statement.executeUpdate("""insert into "CUSTOMER" ("ID", "VERSION", "NAME", "WORKSPACE_ID", "CREATED_AT") values ('mom', 0, 'MomCorp', 'planet', timestamp '1999-03-28 23:01:02')""")
                statement.executeUpdate("""insert into "CATEGORY" ("ID", "VERSION", "NAME", "WORKSPACE_ID", "INCOME", "EXPENSE", "CREATED_AT") values ('delivery', 0, 'Delivery', 'planet', true, true, timestamp '1999-03-28 23:01:02')""")
                statement.executeUpdate("""insert into "DOCUMENT" ("ID", "VERSION", "TIME_UPLOADED", "NAME", "STORAGE_ID", "WORKSPACE_ID", "CREATED_AT", "MIME_TYPE") values ('doc1', 0, timestamp '1999-03-28 23:01:02', 'Mars receipt', 'mars-receipt', 'planet', timestamp '1999-03-28 23:01:02', 'application/pdf')""")
                statement.executeUpdate("""insert into "GENERAL_TAX" ("ID", "VERSION", "TITLE", "RATE_IN_BPS", "WORKSPACE_ID", "CREATED_AT") values ('tax1', 0, 'Earth VAT', 1000, 'planet', timestamp '1999-03-28 23:01:02')""")
                statement.executeUpdate("""insert into "INCOME_TAX_PAYMENT" ("ID", "VERSION", "AMOUNT", "DATE_PAID", "REPORTING_DATE", "TITLE", "WORKSPACE_ID", "CREATED_AT") values ('payment1', 0, 1200, date '3025-01-15', date '3025-01-01', 'Mars delivery taxes', 'planet', timestamp '1999-03-28 23:01:02')""")
                statement.executeUpdate("""insert into "WORKSPACE_ACCESS_TOKEN" ("ID", "VERSION", "REVOKED", "TIME_CREATED", "TOKEN", "VALID_TILL", "WORKSPACE_ID", "CREATED_AT") values ('access1', 0, false, timestamp '1999-03-28 23:01:02', 'planet-access', timestamp '3025-01-15 00:00:00', 'planet', timestamp '1999-03-28 23:01:02')""")
                statement.executeUpdate("""insert into "STANDALONE_DOCUMENT" ("ID", "VERSION", "TITLE", "DOCUMENT_ID", "CREATED_AT") values ('stand1', 0, 'Mars delivery receipt', 'doc1', timestamp '1999-03-28 23:01:02')""")
                statement.executeUpdate("""insert into "INVOICE" ("ID", "VERSION", "CUSTOMER_ID", "TITLE", "DATE_ISSUED", "DUE_DATE", "CURRENCY", "AMOUNT", "GENERAL_TAX_ID", "STATUS", "CREATED_AT") values ('invoice1', 0, 'mom', 'Moon cargo', date '3025-01-15', date '3025-02-15', 'USD', 5000, 'tax1', 'SENT', timestamp '1999-03-28 23:01:02')""")
                statement.executeUpdate("""insert into "EXPENSE" ("ID", "VERSION", "CURRENCY", "DATE_PAID", "ORIGINAL_AMOUNT", "PERCENT_ON_BUSINESS", "CATEGORY_ID", "TITLE", "WORKSPACE_ID", "GENERAL_TAX_ID", "USE_DIFFERENT_EXCHANGE_RATE_FOR_INCOME_TAX_PURPOSES", "STATUS", "CREATED_AT") values ('expense1', 0, 'USD', date '3025-01-15', 300, 100, 'delivery', 'Robot oil', 'planet', 'tax1', false, 'FINALIZED', timestamp '1999-03-28 23:01:02')""")
                statement.executeUpdate("""insert into "SAVED_WORKSPACE_ACCESS_TOKEN" ("ID", "VERSION", "OWNER_ID", "WORKSPACE_ACCESS_TOKEN_ID", "CREATED_AT") values ('saved1', 0, 'fry', 'access1', timestamp '1999-03-28 23:01:02')""")
                statement.executeUpdate("""insert into "INCOME_TAX_PAYMENT_ATTACHMENTS" ("INCOME_TAX_PAYMENT_ID", "DOCUMENT_ID") values ('payment1', 'doc1')""")
                statement.executeUpdate("""insert into "DOCUMENTS_MIGRATION_DOCUMENT" ("MIGRATION_ID", "DOCUMENT_ID") values ('migration1', 'doc1')""")
                statement.executeUpdate("""insert into "INCOME" ("ID", "VERSION", "CURRENCY", "DATE_RECEIVED", "ORIGINAL_AMOUNT", "CATEGORY_ID", "TITLE", "WORKSPACE_ID", "GENERAL_TAX_ID", "USE_DIFFERENT_EXCHANGE_RATE_FOR_INCOME_TAX_PURPOSES", "STATUS", "LINKED_INVOICE_ID", "CREATED_AT") values ('income1', 0, 'USD', date '3025-01-15', 5000, 'delivery', 'Moon cargo payment', 'planet', 'tax1', false, 'FINALIZED', 'invoice1', timestamp '1999-03-28 23:01:02')""")
                statement.executeUpdate("""insert into "EXPENSE_ATTACHMENTS" ("EXPENSE_ID", "DOCUMENT_ID") values ('expense1', 'doc1')""")
                statement.executeUpdate("""insert into "INVOICE_ATTACHMENTS" ("INVOICE_ID", "DOCUMENT_ID") values ('invoice1', 'doc1')""")
                statement.executeUpdate("""insert into "INCOME_ATTACHMENTS" ("INCOME_ID", "DOCUMENT_ID") values ('income1', 'doc1')""")
            }
        }
        val originalFile = source.resolveSibling("simple-accounting.mv.db")
        val originalBytes = Files.readAllBytes(originalFile)

        importer.importFrom(source)

        assertImportedRows()
        dataSource.connection.use { count(it, "sa_h2_import").shouldBe(1) }
        importer.importFrom(source)
        assertImportedRows()
        dataSource.connection.use { count(it, "sa_h2_import").shouldBe(1) }
        Files.readAllBytes(originalFile).contentEquals(originalBytes).shouldBe(true)
    }

    @Test
    fun `rejects a broken foreign key and rolls back every copied row`() {
        val source = createH2Database()
        connectToH2(source).use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("set referential_integrity false")
                statement.executeUpdate("""insert into "WORKSPACE" ("ID", "VERSION", "NAME", "OWNER_ID", "DEFAULT_CURRENCY", "CREATED_AT") values ('planet', 0, 'Planet Express', 'missing', 'USD', timestamp '1999-03-28 23:01:02')""")
                statement.execute("set referential_integrity true")
            }
        }

        shouldThrow<SQLException> { importer.importFrom(source) }

        dataSource.connection.use { connection ->
            count(connection, "workspace").shouldBe(0)
            count(connection, "sa_h2_import").shouldBe(0)
        }
    }

    @Test
    fun `checks PostgreSQL foreign keys before commit`() {
        dataSource.connection.use { connection ->
            connection.autoCommit = false
            try {
                shouldThrow<SQLException> {
                    connection.createStatement().use { statement ->
                        statement.executeUpdate("""insert into workspace (id, version, name, owner_id, default_currency, created_at) values ('planet', 0, 'Planet Express', 'missing', 'USD', timestamp '1999-03-28 23:01:02')""")
                    }
                }
            } finally {
                connection.rollback()
            }
        }
    }

    @Test
    fun `upgrades an older H2 database before importing`() {
        val source = createH2Database(MigrationVersion.fromVersion("0012"))
        connectToH2(source).use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate("""insert into "PLATFORM_USER" ("ID", "VERSION", "USER_NAME", "PASSWORD_HASH", "IS_ADMIN", "FAILED_ATTEMPTS_COUNT", "LANGUAGE", "LOCALE", "ACTIVATED", "CREATED_AT") values ('fry', 0, 'Fry', 'hashed', true, 0, 'en', 'en', true, timestamp '1999-03-28 00:00:00')""")
            }
        }

        importer.importFrom(source)

        assertRow("select user_name from platform_user where id = 'fry'", "Fry")
        dataSource.connection.use { count(it, "oauth_provider").shouldBe(0) }
    }

    @Test
    fun `refuses to import into a populated PostgreSQL database`() {
        val source = createH2Database()
        preconditions { fry() }

        shouldThrow<IllegalArgumentException> { importer.importFrom(source) }

        dataSource.connection.use { connection ->
            count(connection, "platform_user").shouldBe(1)
            count(connection, "sa_h2_import").shouldBe(0)
        }
    }

    private fun assertImportedRows() {
        assertRow("select id, user_name, password_hash from platform_user", "fry", "Fry", "hashed")
        assertRow("select id, user_name, access_token from persistent_oauth2_authorized_client", "client1", "Fry", "slurm-access")
        assertRow("select client_id, access_token_scopes from persistent_oauth2_authorized_client_access_token_scopes", "client1", "openid")
        assertRow("select id, name, client_id from oauth_provider", "provider1", "Planet Express SSO", "px-client")
        assertRow("select provider_id, scope from oauth_provider_scope", "provider1", "openid")
        assertRow("select id, owner_id, name from workspace", "planet", "fry", "Planet Express")
        assertRow("select id, user_id, token from refresh_token", "refresh1", "fry", "slurm-refresh")
        assertRow("select id, user_id, folder_id from google_drive_storage_integration", "drive1", "fry", "mars-deliveries")
        assertRow("select id, user_id, token from user_activation_token", "activate1", "fry", "activate-fry")
        assertRow("select id, user_id, migrated_documents_count from documents_migration", "migration1", "fry", "1")
        assertRow("select user_id, provider_id, external_id from user_oauth_identity", "fry", "provider1", "fry-at-planet-express")
        assertRow("select user_id, provider_id, state from oauth_authentication_request", "fry", "provider1", "state-fry")
        assertRow("select id, workspace_id, name from customer", "mom", "planet", "MomCorp")
        assertRow("select id, workspace_id, name from category", "delivery", "planet", "Delivery")
        assertRow("select id, workspace_id, name from document", "doc1", "planet", "Mars receipt")
        assertRow("select id, workspace_id, title from general_tax", "tax1", "planet", "Earth VAT")
        assertRow("select id, workspace_id, title from income_tax_payment", "payment1", "planet", "Mars delivery taxes")
        assertRow("select id, workspace_id, token from workspace_access_token", "access1", "planet", "planet-access")
        assertRow("select id, document_id, title from standalone_document", "stand1", "doc1", "Mars delivery receipt")
        assertRow("select id, customer_id, general_tax_id, title from invoice", "invoice1", "mom", "tax1", "Moon cargo")
        assertRow("select id, workspace_id, category_id, general_tax_id, title from expense", "expense1", "planet", "delivery", "tax1", "Robot oil")
        assertRow("select id, owner_id, workspace_access_token_id from saved_workspace_access_token", "saved1", "fry", "access1")
        assertRow("select income_tax_payment_id, document_id from income_tax_payment_attachments", "payment1", "doc1")
        assertRow("select migration_id, document_id from documents_migration_document", "migration1", "doc1")
        assertRow("select id, workspace_id, category_id, general_tax_id, linked_invoice_id, title from income", "income1", "planet", "delivery", "tax1", "invoice1", "Moon cargo payment")
        assertRow("select expense_id, document_id from expense_attachments", "expense1", "doc1")
        assertRow("select invoice_id, document_id from invoice_attachments", "invoice1", "doc1")
        assertRow("select income_id, document_id from income_attachments", "income1", "doc1")
    }

    private fun assertRow(sql: String, vararg expected: String) {
        dataSource.connection.use { connection ->
            connection.createStatement().executeQuery(sql).use { rows ->
                rows.next().shouldBe(true)
                rows.metaData.columnCount.shouldBe(expected.size)
                expected.forEachIndexed { index, value -> rows.getString(index + 1).shouldBe(value) }
                rows.next().shouldBe(false)
            }
        }
    }

    private fun createH2Database(targetVersion: MigrationVersion? = null): Path {
        val source = directory.resolve("simple-accounting")
        val configuration = Flyway.configure()
            .dataSource("jdbc:h2:file:$source", "sa", "killallhumans")
            .locations("classpath:db/h2")
        if (targetVersion != null) configuration.target(targetVersion)
        configuration.load().migrate()
        return source
    }

    private fun connectToH2(source: Path): Connection =
        DriverManager.getConnection("jdbc:h2:file:$source;IFEXISTS=TRUE", "sa", "killallhumans")

    private fun count(connection: Connection, table: String): Int =
        connection.createStatement().executeQuery("select count(*) from $table").use { rows ->
            rows.next()
            rows.getInt(1)
        }
}
