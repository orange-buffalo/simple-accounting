package io.orangebuffalo.accounting.simpleaccounting.services.persistence.repos

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.boot.Metadata
import org.hibernate.internal.SessionFactoryImpl
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl
import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor
import org.hibernate.service.ServiceRegistry
import org.hibernate.tool.schema.spi.DelayedDropRegistryNotAvailableImpl
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.Resource
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.io.StringWriter
import java.nio.file.Files
import javax.transaction.Transactional

@ExtendWith(SpringExtension::class)
@SpringBootTest
@Transactional
@TestPropertySource(properties = ["logging.level.org.hibernate.SQL=DEBUG"])
class SchemaValidationIT {

    @Autowired
    private lateinit var localContainerEntityManagerFactoryBean: LocalContainerEntityManagerFactoryBean

    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    @Value("classpath:hibernate-ddl.sql")
    private lateinit var persistedHibernateDdl: Resource

    @Test
    fun `Flyway should produce Hibernate-valid schema`() {
        val schemaToolSetup = getSchemaToolSetup()
        schemaToolSetup.properties["hibernate.hbm2ddl.auto"] = "validate"

        SchemaManagementToolCoordinator.process(
            schemaToolSetup.metadata,
            schemaToolSetup.serviceRegistry,
            schemaToolSetup.properties,
            DelayedDropRegistryNotAvailableImpl.INSTANCE
        )
    }

    @Test
    fun `Hibernate DDL should be up to date for tracking and manual validation purposes`() {
        val schemaToolSetup = getSchemaToolSetup()
        val ddlWriter = StringWriter()
        schemaToolSetup.properties["javax.persistence.schema-generation.scripts.action"] = "create"
        schemaToolSetup.properties["javax.persistence.schema-generation.scripts.create-target"] = ddlWriter

        SchemaManagementToolCoordinator.process(
            schemaToolSetup.metadata,
            schemaToolSetup.serviceRegistry,
            schemaToolSetup.properties,
            DelayedDropRegistryNotAvailableImpl.INSTANCE
        )

        val actualDdl = ddlWriter.toString()
        assertThat(actualDdl.trim())
            .isEqualTo(String(Files.readAllBytes(persistedHibernateDdl.file.toPath())).trim())
    }

    private fun getSchemaToolSetup(): SchemaToolSetup {
        val persistenceUnitInfo = localContainerEntityManagerFactoryBean.persistenceUnitInfo

        val jpaPropertyMap = localContainerEntityManagerFactoryBean.jpaPropertyMap

        val builder = EntityManagerFactoryBuilderImpl(
            PersistenceUnitInfoDescriptor(persistenceUnitInfo),
            jpaPropertyMap,
            SchemaValidationIT::class.java.classLoader
        )

        val managerFactory = builder.build()

        val factoryImpl = managerFactory.unwrap(SessionFactoryImpl::class.java)

        return SchemaToolSetup(
            builder.metadata,
            factoryImpl.serviceRegistry,
            HashMap(builder.configurationValues)
        )
    }

    private data class SchemaToolSetup(
        val metadata: Metadata,
        val serviceRegistry: ServiceRegistry,
        val properties: MutableMap<in Any, in Any>
    )
}