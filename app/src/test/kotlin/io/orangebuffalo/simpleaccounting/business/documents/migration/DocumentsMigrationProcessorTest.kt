package io.orangebuffalo.simpleaccounting.business.documents.migration

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.documents.Document
import io.orangebuffalo.simpleaccounting.tests.infra.ui.TestDocumentsStorage
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findSingle
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.nio.file.Files

class DocumentsMigrationProcessorTest(
    @Autowired private val documentsMigrationProcessor: DocumentsMigrationProcessor,
    @Autowired private val testDocumentsStorage: TestDocumentsStorage,
) : SaIntegrationTestBase() {

    @Test
    fun `should copy document to upload storage update document remove old content and complete migration`() {
        val sourceContent = "Good news, everyone!".toByteArray()
        val testData = preconditions {
            object {
                val fry = platformUser(documentsStorage = TestDocumentsStorage.STORAGE_ID)
                val workspace = workspace(owner = fry)
                val sourceLocation = "migration-source/slurm.txt"
                val document = document(
                    workspace = workspace,
                    storageId = "local-fs",
                    storageLocation = sourceLocation,
                    sizeInBytes = sourceContent.size.toLong(),
                    mimeType = "text/plain",
                )
                val migration = documentsMigration(
                    user = fry,
                    uploadStorageId = TestDocumentsStorage.STORAGE_ID,
                    documentsToMigrate = setOf(document),
                    completedAt = null,
                )
            }
        }
        val sourceFile = localFsStorageProperties.baseDirectory.resolve(testData.sourceLocation)
        Files.createDirectories(sourceFile.parent)
        Files.write(sourceFile, sourceContent)

        runBlocking { documentsMigrationProcessor.processMigration(testData.migration.id!!) }

        val migratedDocument = aggregateTemplate.findSingle<Document>(testData.document.id!!)
        migratedDocument.storageId.shouldBe(TestDocumentsStorage.STORAGE_ID)
        migratedDocument.storageLocation.shouldNotBe(testData.sourceLocation)
        migratedDocument.sizeInBytes.shouldBe(sourceContent.size.toLong())
        testDocumentsStorage.getUploadedContent(migratedDocument.storageLocation!!).shouldBe(sourceContent)
        Files.exists(sourceFile).shouldBeFalse()

        val migration = aggregateTemplate.findSingle<DocumentsMigration>(testData.migration.id!!)
        migration.migratedDocumentsCount.shouldBe(1)
        migration.completedAt.shouldBe(MOCK_TIME)
    }

    @Test
    fun `should count failed document and continue with remaining documents`() {
        val sourceContent = "Delivery to Omicron Persei 8".toByteArray()
        val testData = preconditions {
            object {
                val fry = platformUser(documentsStorage = TestDocumentsStorage.STORAGE_ID)
                val workspace = workspace(owner = fry)
                val missingDocument = document(
                    workspace = workspace,
                    storageId = "local-fs",
                    storageLocation = "migration-source/missing.txt",
                    mimeType = "text/plain",
                )
                val sourceLocation = "migration-source/planet-express.txt"
                val migratedDocument = document(
                    workspace = workspace,
                    storageId = "local-fs",
                    storageLocation = sourceLocation,
                    sizeInBytes = sourceContent.size.toLong(),
                    mimeType = "text/plain",
                )
                val migration = documentsMigration(
                    user = fry,
                    uploadStorageId = TestDocumentsStorage.STORAGE_ID,
                    documentsToMigrate = setOf(missingDocument, migratedDocument),
                    completedAt = null,
                )
            }
        }
        val sourceFile = localFsStorageProperties.baseDirectory.resolve(testData.sourceLocation)
        Files.createDirectories(sourceFile.parent)
        Files.write(sourceFile, sourceContent)

        runBlocking { documentsMigrationProcessor.processMigration(testData.migration.id!!) }

        val failedDocument = aggregateTemplate.findSingle<Document>(testData.missingDocument.id!!)
        failedDocument.storageId.shouldBe("local-fs")
        failedDocument.storageLocation.shouldBe("migration-source/missing.txt")

        val migratedDocument = aggregateTemplate.findSingle<Document>(testData.migratedDocument.id!!)
        migratedDocument.storageId.shouldBe(TestDocumentsStorage.STORAGE_ID)
        testDocumentsStorage.getUploadedContent(migratedDocument.storageLocation!!).shouldBe(sourceContent)

        val migration = aggregateTemplate.findSingle<DocumentsMigration>(testData.migration.id!!)
        migration.migratedDocumentsCount.shouldBe(2)
        migration.completedAt.shouldBe(MOCK_TIME)
    }

    @Test
    fun `should keep old location when copy to upload storage fails`() {
        val sourceContent = "Robot oil receipt".toByteArray()
        val testData = preconditions {
            object {
                val fry = platformUser(documentsStorage = "google-drive")
                val workspace = workspace(owner = fry)
                val sourceLocation = "migration-source/robot-oil.txt"
                val document = document(
                    workspace = workspace,
                    storageId = "local-fs",
                    storageLocation = sourceLocation,
                    sizeInBytes = sourceContent.size.toLong(),
                    mimeType = "text/plain",
                )
                val migration = documentsMigration(
                    user = fry,
                    uploadStorageId = "google-drive",
                    documentsToMigrate = setOf(document),
                    completedAt = null,
                )
            }
        }
        val sourceFile = localFsStorageProperties.baseDirectory.resolve(testData.sourceLocation)
        Files.createDirectories(sourceFile.parent)
        Files.write(sourceFile, sourceContent)

        runBlocking { documentsMigrationProcessor.processMigration(testData.migration.id!!) }

        val document = aggregateTemplate.findSingle<Document>(testData.document.id!!)
        document.storageId.shouldBe("local-fs")
        document.storageLocation.shouldBe(testData.sourceLocation)
        Files.readAllBytes(sourceFile).shouldBe(sourceContent)

        val migration = aggregateTemplate.findSingle<DocumentsMigration>(testData.migration.id!!)
        migration.migratedDocumentsCount.shouldBe(1)
        migration.completedAt.shouldBe(MOCK_TIME)
    }
}
