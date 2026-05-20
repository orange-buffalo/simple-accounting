package io.orangebuffalo.simpleaccounting.business.documents.migration

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.orangebuffalo.simpleaccounting.SaIntegrationTestBase
import io.orangebuffalo.simpleaccounting.business.documents.Document
import io.orangebuffalo.simpleaccounting.business.documents.storage.gdrive.GoogleDriveStorageIntegration
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.GoogleDriveApiMocks
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.GoogleOAuthMocks
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.ThirdPartyApisMocksContextInitializer
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.ThirdPartyApisMocksListener
import io.orangebuffalo.simpleaccounting.tests.infra.ui.TestDocumentsStorage
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findSingle
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeEntityWithFields
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.mono
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestExecutionListeners
import org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS
import org.springframework.web.server.ServerWebExchange
import reactor.util.context.Context
import java.nio.file.Files
import java.nio.file.Path

@TestExecutionListeners(
    listeners = [ThirdPartyApisMocksListener::class],
    mergeMode = MERGE_WITH_DEFAULTS,
)
@ContextConfiguration(initializers = [ThirdPartyApisMocksContextInitializer::class])
class DocumentsMigrationProcessorTest(
    @Autowired private val documentsMigrationProcessor: DocumentsMigrationProcessor,
    @Autowired private val testDocumentsStorage: TestDocumentsStorage,
) : SaIntegrationTestBase() {

    @TempDir
    private lateinit var localFsDir: Path

    @BeforeEach
    fun setupDocumentsMigrationProcessorTest() {
        whenever(localFsStorageProperties.baseDirectory) doReturn localFsDir
    }

    private suspend fun processMigration(migrationId: String) {
        val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/documents/migration").build())
        mono { documentsMigrationProcessor.processMigration(migrationId) }
            .contextWrite(Context.of(ServerWebExchange::class.java, exchange))
            .awaitSingle()
    }

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
                    documentsToMigrate = setOf(document),
                    completedAt = null,
                )
            }
        }
        val sourceFile = localFsDir.resolve(testData.sourceLocation)
        Files.createDirectories(sourceFile.parent)
        Files.write(sourceFile, sourceContent)

        runBlocking { processMigration(testData.migration.id!!) }

        val migratedDocument = aggregateTemplate.findSingle<Document>(testData.document.id!!)
        migratedDocument.storageLocation.shouldNotBe(testData.sourceLocation)
        migratedDocument.shouldBeEntityWithFields(
            testData.document.copy(
                storageId = TestDocumentsStorage.STORAGE_ID,
                storageLocation = migratedDocument.storageLocation,
                sizeInBytes = sourceContent.size.toLong(),
            )
        )
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
                // This document intentionally points to a missing local-fs file to exercise per-document failure handling.
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
                    documentsToMigrate = setOf(missingDocument, migratedDocument),
                    completedAt = null,
                )
            }
        }
        val sourceFile = localFsDir.resolve(testData.sourceLocation)
        Files.createDirectories(sourceFile.parent)
        Files.write(sourceFile, sourceContent)

        runBlocking { processMigration(testData.migration.id!!) }

        aggregateTemplate.findSingle<Document>(testData.missingDocument.id!!)
            .shouldBeEntityWithFields(testData.missingDocument)

        val migratedDocument = aggregateTemplate.findSingle<Document>(testData.migratedDocument.id!!)
        migratedDocument.shouldBeEntityWithFields(
            testData.migratedDocument.copy(
                storageId = TestDocumentsStorage.STORAGE_ID,
                storageLocation = migratedDocument.storageLocation,
                sizeInBytes = sourceContent.size.toLong(),
            )
        )
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
                // Google Drive is selected but deliberately not authorized, so saving into the target storage fails.
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
                    documentsToMigrate = setOf(document),
                    completedAt = null,
                )
            }
        }
        val sourceFile = localFsDir.resolve(testData.sourceLocation)
        Files.createDirectories(sourceFile.parent)
        Files.write(sourceFile, sourceContent)

        runBlocking { processMigration(testData.migration.id!!) }

        aggregateTemplate.findSingle<Document>(testData.document.id!!)
            .shouldBeEntityWithFields(testData.document)
        Files.readAllBytes(sourceFile).shouldBe(sourceContent)

        val migration = aggregateTemplate.findSingle<DocumentsMigration>(testData.migration.id!!)
        migration.migratedDocumentsCount.shouldBe(1)
        migration.completedAt.shouldBe(MOCK_TIME)
    }

    @Test
    fun `should keep documents already in upload storage untouched`() {
        val sourceContent = "Delivery permit".toByteArray()
        val testData = preconditions {
            object {
                val fry = platformUser(documentsStorage = TestDocumentsStorage.STORAGE_ID)
                val workspace = workspace(owner = fry)
                val sourceLocation = "migration-source/delivery-permit.txt"
                val sourceDocument = document(
                    workspace = workspace,
                    storageId = "local-fs",
                    storageLocation = sourceLocation,
                    sizeInBytes = sourceContent.size.toLong(),
                    mimeType = "text/plain",
                )
                val uploadedDocument = document(
                    workspace = workspace,
                    storageId = TestDocumentsStorage.STORAGE_ID,
                    storageLocation = "already-uploaded/slurm-receipt.txt",
                    sizeInBytes = 42,
                    mimeType = "text/plain",
                )
                val migration = documentsMigration(
                    user = fry,
                    documentsToMigrate = setOf(sourceDocument),
                    completedAt = null,
                )
            }
        }
        val sourceFile = localFsDir.resolve(testData.sourceLocation)
        Files.createDirectories(sourceFile.parent)
        Files.write(sourceFile, sourceContent)

        runBlocking { processMigration(testData.migration.id!!) }

        aggregateTemplate.findSingle<Document>(testData.uploadedDocument.id!!)
            .shouldBeEntityWithFields(testData.uploadedDocument)

        val migratedDocument = aggregateTemplate.findSingle<Document>(testData.sourceDocument.id!!)
        migratedDocument.shouldBeEntityWithFields(
            testData.sourceDocument.copy(
                storageId = TestDocumentsStorage.STORAGE_ID,
                storageLocation = migratedDocument.storageLocation,
                sizeInBytes = sourceContent.size.toLong(),
            )
        )
        testDocumentsStorage.getUploadedContent(migratedDocument.storageLocation!!).shouldBe(sourceContent)

        val migration = aggregateTemplate.findSingle<DocumentsMigration>(testData.migration.id!!)
        migration.migratedDocumentsCount.shouldBe(1)
        migration.completedAt.shouldBe(MOCK_TIME)
    }

    @Nested
    inner class SupportedStoragesMigration {

        @Test
        fun `should migrate from all supported storages to local fs`() {
            val sourceContent = "Leela delivery contract".toByteArray()
            val testData = preconditions {
                object {
                    val leela = platformUser(documentsStorage = "local-fs").also {
                        save(GoogleDriveStorageIntegration(userId = it.id!!, folderId = "root-folder-id"))
                    }
                    val workspace = workspace(owner = leela)
                    val sourceDocument = document(
                        workspace = workspace,
                        name = "leela-contract.txt",
                        storageId = "google-drive",
                        storageLocation = "gdrive-source-file-id",
                        sizeInBytes = sourceContent.size.toLong(),
                        mimeType = "text/plain",
                    )
                    val migration = documentsMigration(
                        user = leela,
                        documentsToMigrate = setOf(sourceDocument),
                        completedAt = null,
                    )
                }
            }
            val accessToken = GoogleOAuthMocks.token().enqueue().persist(testData.leela)
            GoogleDriveApiMocks.mockDownloadFile(
                fileId = "gdrive-source-file-id",
                content = sourceContent,
                expectedAuthToken = accessToken,
            )
            GoogleDriveApiMocks.mockDeleteFile(
                fileId = "gdrive-source-file-id",
                expectedAuthToken = accessToken,
            )

            runBlocking { processMigration(testData.migration.id!!) }

            val migratedDocument = aggregateTemplate.findSingle<Document>(testData.sourceDocument.id!!)
            migratedDocument.shouldBeEntityWithFields(
                testData.sourceDocument.copy(
                    storageId = "local-fs",
                    storageLocation = migratedDocument.storageLocation,
                    sizeInBytes = sourceContent.size.toLong(),
                )
            )
            Files.readAllBytes(localFsDir.resolve(migratedDocument.storageLocation!!)).shouldBe(sourceContent)
            GoogleDriveApiMocks.verifyDeleteFileRequest("gdrive-source-file-id")
        }

        @Test
        fun `should migrate from all supported storages to google drive`() {
            val sourceContent = "Bender bending permit".toByteArray()
            val testData = preconditions {
                object {
                    val bender = platformUser(documentsStorage = "google-drive").also {
                        save(GoogleDriveStorageIntegration(userId = it.id!!, folderId = "root-folder-id"))
                    }
                    val workspace = workspace(owner = bender)
                    val sourceLocation = "migration-source/bender-permit.txt"
                    val sourceDocument = document(
                        workspace = workspace,
                        name = "bender-permit.txt",
                        storageId = "local-fs",
                        storageLocation = sourceLocation,
                        sizeInBytes = sourceContent.size.toLong(),
                        mimeType = "text/plain",
                    )
                    val migration = documentsMigration(
                        user = bender,
                        documentsToMigrate = setOf(sourceDocument),
                        completedAt = null,
                    )
                }
            }
            val sourceFile = localFsDir.resolve(testData.sourceLocation)
            Files.createDirectories(sourceFile.parent)
            Files.write(sourceFile, sourceContent)

            val accessToken = GoogleOAuthMocks.token().enqueue().persist(testData.bender)
            GoogleDriveApiMocks.mockFindFolder(
                parentFolderId = "root-folder-id",
                folderName = testData.workspace.id.toString(),
                responseFolderId = "workspace-folder-id",
                expectedAuthToken = accessToken,
            )
            GoogleDriveApiMocks.mockUploadFileForFileName(
                fileName = "bender-permit_${MOCK_TIME.toEpochMilli()}.txt",
                responseId = "gdrive-uploaded-file-id",
                responseSize = sourceContent.size.toLong(),
                expectedAuthToken = accessToken,
            )

            runBlocking { processMigration(testData.migration.id!!) }

            aggregateTemplate.findSingle<Document>(testData.sourceDocument.id!!)
                .shouldBeEntityWithFields(
                    testData.sourceDocument.copy(
                        storageId = "google-drive",
                        storageLocation = "gdrive-uploaded-file-id",
                        sizeInBytes = sourceContent.size.toLong(),
                    )
                )
            Files.exists(sourceFile).shouldBeFalse()
            GoogleDriveApiMocks.verifyUploadFileRequest()
        }
    }
}
