package io.orangebuffalo.accounting.simpleaccounting.services.persistence

import io.orangebuffalo.accounting.simpleaccounting.services.persistence.admin.ADMIN_SCHEMA_NAME
import org.flywaydb.core.Flyway
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct
import javax.sql.DataSource

@Service
class DatabaseMigrationService(
    private val appDataSource: DataSource,
    private val multiTenancyService: MultiTenancyService
) {

    @PostConstruct
    fun init() {
        migrateAdminSchema()
        migrateUserSchemas()
    }

    private fun migrateUserSchemas() {
        multiTenancyService.getTenantsSchemas()
            .forEach(this::migrateUserSchema)
    }

    private fun migrateUserSchema(schemaName: String) {
        migrateSchema(schemaName, "classpath:db/migration/user")
    }

    private fun migrateAdminSchema() {
        migrateSchema(ADMIN_SCHEMA_NAME, "classpath:db/migration/admin")
    }

    private fun migrateSchema(schemaName: String, migrationLocation: String) {
        with(Flyway()) {
            dataSource = appDataSource
            setLocations(migrationLocation)
            setSchemas(schemaName)
            migrate()
        }
    }
}