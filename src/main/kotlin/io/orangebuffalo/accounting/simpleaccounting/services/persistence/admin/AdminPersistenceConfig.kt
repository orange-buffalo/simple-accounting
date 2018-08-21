package io.orangebuffalo.accounting.simpleaccounting.services.persistence.admin

import org.hibernate.cfg.AvailableSettings
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder
import org.springframework.boot.orm.jpa.hibernate.SpringPhysicalNamingStrategy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import javax.persistence.PersistenceContext
import javax.sql.DataSource

// Unfortunately, flyway generates quoted names for schemas, default H2 connection puts query to uppercase,
// and Hibernate does not quotes by default. To make it all work together, the easiest way it to put the schema
// name into upper case here
const val ADMIN_SCHEMA_NAME = "ADMIN"
const val ADMIN_PERSISTENCE_CONTEXT = "admin"
const val ADMIN_ENTITY_MANAGER = "adminEntityManager"
const val ADMIN_TRANSACTION_MANAGER = "adminTransactionManager"

@Configuration
@EnableJpaRepositories(
    basePackageClasses = [AdminPersistenceConfig::class],
    entityManagerFactoryRef = ADMIN_ENTITY_MANAGER,
    transactionManagerRef = ADMIN_TRANSACTION_MANAGER)
@EnableTransactionManagement
class AdminPersistenceConfig(private val dataSource: DataSource) {

    @PersistenceContext(unitName = ADMIN_PERSISTENCE_CONTEXT)
    @Bean(ADMIN_ENTITY_MANAGER)
    @DependsOn("databaseMigrationService")
    fun adminEntityManagerFactory(builder: EntityManagerFactoryBuilder): LocalContainerEntityManagerFactoryBean =
        builder
            .dataSource(dataSource)
            .properties(mapOf(
                AvailableSettings.PHYSICAL_NAMING_STRATEGY to SpringPhysicalNamingStrategy(),
                AvailableSettings.DEFAULT_SCHEMA to ADMIN_SCHEMA_NAME
            ))
            .persistenceUnit(ADMIN_PERSISTENCE_CONTEXT)
            .packages(AdminPersistenceConfig::class.java.`package`.name)
            .build()

    @Bean(ADMIN_TRANSACTION_MANAGER)
    fun adminTransactionManager(
        @Qualifier(ADMIN_ENTITY_MANAGER) entityManagerFactory: LocalContainerEntityManagerFactoryBean
    ): PlatformTransactionManager = JpaTransactionManager(entityManagerFactory.`object`!!)

}