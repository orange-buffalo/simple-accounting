package io.orangebuffalo.simpleaccounting.business.ui.user.documents

import com.microsoft.playwright.Page
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldBeNull
import io.orangebuffalo.simpleaccounting.business.documents.Document
import io.orangebuffalo.simpleaccounting.business.documents.storage.gdrive.GoogleDriveStorageIntegration
import io.orangebuffalo.simpleaccounting.business.standalonedocuments.StandaloneDocument
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.user.documents.EditStandaloneDocumentPage.Companion.shouldBeEditStandaloneDocumentPage
import io.orangebuffalo.simpleaccounting.business.ui.user.documents.DocumentsOverviewPage.Companion.openDocumentsOverviewPage
import io.orangebuffalo.simpleaccounting.business.ui.user.documents.DocumentsOverviewPage.Companion.shouldBeDocumentsOverviewPage
import io.orangebuffalo.simpleaccounting.business.ui.user.expenses.EditExpensePage.Companion.shouldBeEditExpensePage
import io.orangebuffalo.simpleaccounting.business.ui.user.incomes.EditIncomePage.Companion.shouldBeEditIncomePage
import io.orangebuffalo.simpleaccounting.business.ui.user.incomes.EditIncomeTaxPaymentPage.Companion.shouldBeEditIncomeTaxPaymentPage
import io.orangebuffalo.simpleaccounting.business.ui.user.invoices.EditInvoicePage.Companion.shouldBeEditInvoicePage
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.GoogleDriveApiMocks
import io.orangebuffalo.simpleaccounting.tests.infra.thirdparty.GoogleOAuthMocks
import io.orangebuffalo.simpleaccounting.tests.infra.ui.TestDocumentsStorage
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaOverviewItem.Companion.primaryAttribute
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaOverviewItem.Companion.previewIcons
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaOverviewItemData
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaIconType
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.shouldHaveSideMenu
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.shouldHaveTitles
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.tests.infra.utils.downloadBytes
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findSingle
import io.orangebuffalo.simpleaccounting.tests.infra.utils.withBlockedGqlApiResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlin.io.path.exists
import kotlin.io.path.writeBytes

class DocumentsOverviewFullStackTest : SaFullStackTestBase() {

    @TempDir
    private lateinit var tempDir: Path

    @BeforeEach
    fun setupLocalFsStorage() {
        whenever(localFsStorageProperties.baseDirectory) doReturn tempDir
    }

    @Test
    fun `should not offer standalone document upload to workspace token users`(page: Page) {
        val testData = preconditions {
            object {
                val token = fry().let { fry ->
                    workspaceAccessToken(
                        workspace = workspace(owner = fry),
                        token = "document-overview-token",
                        validTill = Instant.parse("9999-12-31T23:59:59Z"),
                        timeCreated = MOCK_TIME,
                    ).token
                }
            }
        }

        page.navigate("/login-by-link/${testData.token}")
        page.shouldHaveSideMenu().clickDocuments()
        page.shouldBeDocumentsOverviewPage {
            uploadButton.shouldBeHidden()
        }
    }

    @Test
    fun `should display documents with all possible attribute variations`(page: Page) {
        val testData = preconditions {
            object {
                val fry = platformUser(
                    userName = "Fry",
                    isAdmin = false,
                    activated = true,
                    documentsStorage = "noop"
                )

                init {
                    val workspace = workspace(owner = fry)

                    document(
                        workspace = workspace,
                        name = "Unused Receipt.pdf",
                        createdAt = MOCK_TIME,
                    )

                    val singleUsageDoc = document(
                        workspace = workspace,
                        name = "Single Usage Doc.pdf",
                        createdAt = MOCK_TIME.plusSeconds(1),
                        timeUploaded = Instant.parse("3025-01-15T21:30:00Z"),
                    )
                    expense(
                        workspace = workspace,
                        title = "Slurm supplies",
                        attachments = setOf(singleUsageDoc),
                    )

                    val multiUsageDoc = document(
                        workspace = workspace,
                        name = "Multi Usage Doc.pdf",
                        createdAt = MOCK_TIME.plusSeconds(2),
                        timeUploaded = Instant.parse("3025-01-14T09:00:00Z"),
                    )
                    expense(
                        workspace = workspace,
                        title = "Robot oil",
                        attachments = setOf(multiUsageDoc),
                    )
                    income(
                        workspace = workspace,
                        title = "Delivery commission",
                        attachments = setOf(multiUsageDoc),
                    )

                    document(
                        workspace = workspace,
                        name = "Google Drive Doc.pdf",
                        createdAt = MOCK_TIME.plusSeconds(3),
                        timeUploaded = Instant.parse("3025-01-13T08:00:00Z"),
                        storageId = "google-drive",
                    )

                    val standaloneDoc = document(
                        workspace = workspace,
                        name = "Standalone License.pdf",
                        createdAt = MOCK_TIME.plusSeconds(4),
                        timeUploaded = Instant.parse("3025-01-12T07:00:00Z"),
                    )
                    standaloneDocument(
                        workspace = workspace,
                        document = standaloneDoc,
                        title = "Planet Express License",
                    )
                }
            }
        }

        page.authenticateViaCookie(testData.fry)

        page.withBlockedGqlApiResponse(
            "documentsPage",
            initiator = {
                page.openDocumentsOverviewPage { }
            },
            blockedRequestSpec = {
                page.shouldBeDocumentsOverviewPage {
                    pageItems.shouldHaveLoadingIndicatorVisible()
                    reportRendering("documents-overview.loading")
                }
            }
        )

        page.shouldBeDocumentsOverviewPage {
            val documentsOverviewPage = this
            pageItems {
                shouldHaveExactData(
                    SaOverviewItemData(
                        title = "Standalone License.pdf",
                        primaryAttributes = primaryAttributes(
                            primaryAttribute(SaIconType.ATTACHMENT, text = "Planet Express License"),
                        ),
                        attributePreviewIcons = previewIcons(SaIconType.CALENDAR, SaIconType.DATA_LINE),
                        lastColumnContent = lastColumnActions(),
                        hasDetails = false,
                    ),
                    SaOverviewItemData(
                        title = "Google Drive Doc.pdf",
                        primaryAttributes = primaryAttributes(),
                        attributePreviewIcons = previewIcons(SaIconType.CALENDAR, SaIconType.GOOGLE_DRIVE),
                        lastColumnContent = lastColumnActions(),
                        hasDetails = false,
                    ),
                    SaOverviewItemData(
                        title = "Multi Usage Doc.pdf",
                        primaryAttributes = primaryAttributes(
                            primaryAttribute(SaIconType.EXPENSE, text = "Robot oil"),
                            primaryAttribute(SaIconType.INCOME, text = "Delivery commission"),
                        ),
                        attributePreviewIcons = previewIcons(SaIconType.CALENDAR, SaIconType.DATA_LINE),
                        lastColumnContent = lastColumnActions(),
                        hasDetails = false,
                    ),
                    SaOverviewItemData(
                        title = "Single Usage Doc.pdf",
                        primaryAttributes = primaryAttributes(
                            primaryAttribute(SaIconType.EXPENSE, text = "Slurm supplies"),
                        ),
                        attributePreviewIcons = previewIcons(SaIconType.CALENDAR, SaIconType.DATA_LINE),
                        lastColumnContent = lastColumnActions(),
                        hasDetails = false,
                    ),
                    SaOverviewItemData(
                        title = "Unused Receipt.pdf",
                        primaryAttributes = primaryAttributes(),
                        attributePreviewIcons = previewIcons(SaIconType.CALENDAR, SaIconType.DATA_LINE),
                        lastColumnContent = lastColumnActions(),
                        hasDetails = false,
                    ),
                )

                paginator {
                    shouldHaveActivePage(1)
                    shouldHaveTotalPages(1)
                }
            }
            reportRendering("documents-overview.loaded")

            pageItems {
                val unusedDocument = shouldHaveItemSatisfying { it.title == "Unused Receipt.pdf" }
                unusedDocument.shouldHaveActionMenuItems("Download", "Delete")
                unusedDocument.shouldHaveLastColumnActionEnabled("Download")
                unusedDocument.shouldHaveLastColumnActionEnabled("Delete")

                shouldHaveItemSatisfying { it.title == "Single Usage Doc.pdf" }.also {
                    it.shouldHaveActionMenuItems("Download", "Navigate to Slurm supplies")
                    it.shouldHaveLastColumnActionEnabled("Download")
                }
                shouldHaveItemSatisfying { it.title == "Standalone License.pdf" }.also {
                    it.shouldHaveActionMenuItems("Download", "Navigate to Planet Express License", "Delete")
                    it.shouldHaveLastColumnActionEnabled("Delete")
                    documentsOverviewPage.reportRenderingWithPopovers("documents-overview.actions-menu")
                    it.clickActionMenuItem("Navigate to Planet Express License")
                }
            }
        }

        page.shouldBeEditStandaloneDocumentPage {
            title { input.shouldHaveValue("Planet Express License") }
        }
    }

    private fun primaryAttributes(vararg usages: String) = listOf(
        *usages,
    )

    private fun lastColumnActions(): String? = null

    @Test
    fun `should disable download while document storages status is loading`(page: Page) {
        val testData = preconditions {
            object {
                val fry = platformUser(
                    userName = "Fry",
                    documentsStorage = TestDocumentsStorage.STORAGE_ID,
                )

                init {
                    document(
                        workspace = workspace(owner = fry),
                        name = "loading-storage-receipt.pdf",
                        storageId = TestDocumentsStorage.STORAGE_ID,
                        storageLocation = "loading-storage-receipt-location",
                    )
                }
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.withBlockedGqlApiResponse(
            "downloadDocumentStorages",
            initiator = {
                page.openDocumentsOverviewPage { }
            },
            blockedRequestSpec = {
                page.shouldBeDocumentsOverviewPage {
                    pageItems {
                        val documentItem = shouldHaveItemSatisfying { it.title == "loading-storage-receipt.pdf" }
                        documentItem.shouldHaveLastColumnActionDisabled("Download")
                        documentItem.shouldHaveLastColumnActionTooltip(
                            "Download",
                            "Waiting for the storage to become available",
                        )
                    }
                }
            }
        )
    }

    @Test
    fun `should disable download when document storage is inactive`(page: Page) {
        val testData = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = "google-drive")

                init {
                    document(
                        workspace = workspace(owner = fry),
                        name = "inactive-gdrive-contract.pdf",
                        storageId = "google-drive",
                        storageLocation = "inactive-gdrive-contract-file-id",
                    )
                }
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.openDocumentsOverviewPage {
            pageItems {
                val documentItem = shouldHaveItemSatisfying { it.title == "inactive-gdrive-contract.pdf" }
                documentItem.shouldHaveLastColumnActionDisabled("Download")
                documentItem.shouldHaveLastColumnActionTooltip(
                    "Download",
                    "You need to (re-)activate the documents storage to download this document. " +
                            "Navigate to your profile settings and check there.",
                )
            }
        }
    }

    @Test
    fun `should delete unused document from overview`(page: Page) {
        val testData = preconditions {
            object {
                val fry = platformUser(
                    userName = "Fry",
                    documentsStorage = TestDocumentsStorage.STORAGE_ID,
                )
                val workspace = workspace(owner = fry)
                val unusedDocument = document(
                    workspace = workspace,
                    name = "Unused Slurm Receipt.pdf",
                    storageId = TestDocumentsStorage.STORAGE_ID,
                    storageLocation = "unused-slurm-receipt-location",
                )

                init {
                    val usedDocument = document(
                        workspace = workspace,
                        name = "Used Robot Oil Receipt.pdf",
                        storageId = TestDocumentsStorage.STORAGE_ID,
                        storageLocation = "used-robot-oil-receipt-location",
                    )
                    expense(
                        workspace = workspace,
                        title = "Robot oil",
                        attachments = setOf(usedDocument),
                    )
                }
            }
        }
        testDocumentsStorage.mockDocumentContent(
            "unused-slurm-receipt-location",
            "Good news, everyone! unused Slurm receipt".toByteArray()
        )

        page.authenticateViaCookie(testData.fry)
        page.openDocumentsOverviewPage {
            pageItems {
                val unusedItem = shouldHaveItemSatisfying { it.title == "Unused Slurm Receipt.pdf" }
                shouldHaveItemSatisfying { it.title == "Used Robot Oil Receipt.pdf" }
                    .shouldHaveActionMenuItems("Download", "Navigate to Robot oil")
                unusedItem.clickActionMenuItem("Delete")
            }
            confirmDocumentDeletion()
            pageItems.shouldHaveTitles("Used Robot Oil Receipt.pdf")
        }

        aggregateTemplate.findById(testData.unusedDocument.id!!, Document::class.java).shouldBeNull()
        testDocumentsStorage.hasUploadedContent("unused-slurm-receipt-location").shouldBeFalse()
    }

    @Test
    fun `should remove standalone document and unused linked document from overview`(page: Page) {
        val testData = preconditions {
            object {
                val fry = platformUser(
                    userName = "Fry",
                    documentsStorage = TestDocumentsStorage.STORAGE_ID,
                )
                val workspace = workspace(owner = fry)
                val document = document(
                    workspace = workspace,
                    name = "Standalone delivery permit.pdf",
                    storageId = TestDocumentsStorage.STORAGE_ID,
                    storageLocation = "standalone-delivery-permit-location",
                )
                val standaloneDocument = standaloneDocument(
                    workspace = workspace,
                    document = document,
                    title = "Planet Express delivery permit",
                )
            }
        }
        testDocumentsStorage.mockDocumentContent(
            "standalone-delivery-permit-location",
            "Good news, everyone! Standalone delivery permit".toByteArray()
        )

        page.authenticateViaCookie(testData.fry)
        page.openDocumentsOverviewPage {
            pageItems {
                val standaloneItem = shouldHaveItemSatisfying { it.title == "Standalone delivery permit.pdf" }
                standaloneItem.shouldHaveActionMenuItems(
                    "Download",
                    "Navigate to Planet Express delivery permit",
                    "Delete",
                )
                standaloneItem.shouldHaveLastColumnActionEnabled("Download")
                standaloneItem.shouldHaveLastColumnActionEnabled("Delete")
                standaloneItem.clickActionMenuItem("Delete")
            }
            confirmDocumentDeletion()
            pageItems.shouldHaveTitles()
        }

        aggregateTemplate.findById(testData.standaloneDocument.id!!, StandaloneDocument::class.java).shouldBeNull()
        aggregateTemplate.findById(testData.document.id!!, Document::class.java).shouldBeNull()
        testDocumentsStorage.hasUploadedContent("standalone-delivery-permit-location").shouldBeFalse()
    }

    @Test
    fun `should download document from test storage`(page: Page) {
        val documentContent = "Good news, everyone! Slurm receipt from test storage".toByteArray()
        val testData = preconditions {
            object {
                val fry = platformUser(
                    userName = "Fry",
                    documentsStorage = TestDocumentsStorage.STORAGE_ID,
                )
                val workspace = workspace(owner = fry)

                init {
                    document(
                        workspace = workspace,
                        name = "slurm-test-storage-receipt.pdf",
                        storageId = TestDocumentsStorage.STORAGE_ID,
                        storageLocation = "slurm-test-storage-receipt-location",
                        sizeInBytes = documentContent.size.toLong(),
                        timeUploaded = MOCK_TIME,
                        mimeType = "application/pdf",
                    )
                }
            }
        }
        testDocumentsStorage.mockDocumentContent("slurm-test-storage-receipt-location", documentContent)

        page.authenticateViaCookie(testData.fry)
        page.openDocumentsOverviewPage {
            pageItems {
                val documentItem = shouldHaveItemSatisfying { it.title == "slurm-test-storage-receipt.pdf" }
                page.downloadBytes {
                    documentItem.clickLastColumnAction("Download")
                }.shouldBe(documentContent)
            }
        }
    }

    @Test
    fun `should download document from local file system`(page: Page) {
        val documentContent = "Good news, everyone! Local delivery contract for Omicron Persei 8".toByteArray()
        val testData = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = "local-fs")
                val workspace = workspace(owner = fry)

                init {
                    document(
                        workspace = workspace,
                        name = "local-delivery-contract.pdf",
                        storageId = "local-fs",
                        storageLocation = "${workspace.id}/local-delivery-contract.pdf",
                        sizeInBytes = documentContent.size.toLong(),
                        timeUploaded = MOCK_TIME,
                        mimeType = "application/pdf",
                    )
                }
            }
        }
        val storedFile = tempDir.resolve("${testData.workspace.id}/local-delivery-contract.pdf")
        Files.createDirectories(storedFile.parent)
        storedFile.writeBytes(documentContent)

        page.authenticateViaCookie(testData.fry)
        page.openDocumentsOverviewPage {
            pageItems {
                val documentItem = shouldHaveItemSatisfying { it.title == "local-delivery-contract.pdf" }
                page.downloadBytes {
                    documentItem.clickLastColumnAction("Download")
                }.shouldBe(documentContent)
            }
        }
    }

    @Test
    fun `should delete document from local file system`(page: Page) {
        val documentContent = "Good news, everyone! Local document to delete".toByteArray()
        val testData = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = "local-fs")
                val workspace = workspace(owner = fry)
                val document = document(
                    workspace = workspace,
                    name = "local-deletable-contract.pdf",
                    storageId = "local-fs",
                    storageLocation = "${workspace.id}/local-deletable-contract.pdf",
                    sizeInBytes = documentContent.size.toLong(),
                    timeUploaded = MOCK_TIME,
                    mimeType = "application/pdf",
                )
            }
        }
        val storedFile = tempDir.resolve("${testData.workspace.id}/local-deletable-contract.pdf")
        Files.createDirectories(storedFile.parent)
        storedFile.writeBytes(documentContent)

        page.authenticateViaCookie(testData.fry)
        page.openDocumentsOverviewPage {
            pageItems {
                shouldHaveItemSatisfying { it.title == "local-deletable-contract.pdf" }
                    .clickActionMenuItem("Delete")
            }
            confirmDocumentDeletion()
            pageItems.shouldHaveTitles()
        }

        aggregateTemplate.findById(testData.document.id!!, Document::class.java).shouldBeNull()
        storedFile.exists().shouldBeFalse()
    }

    @Test
    fun `should download document from Google Drive`(page: Page) {
        val documentContent = "Good news, everyone! Google Drive delivery contract for Mars".toByteArray()
        val testData = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = "google-drive").also {
                    save(
                        GoogleDriveStorageIntegration(
                            userId = it.id!!,
                            folderId = "root-folder-id",
                        )
                    )
                }
                val workspace = workspace(owner = fry)

                init {
                    document(
                        workspace = workspace,
                        name = "gdrive-delivery-contract.pdf",
                        storageId = "google-drive",
                        storageLocation = "gdrive-delivery-contract-file-id",
                        sizeInBytes = documentContent.size.toLong(),
                        timeUploaded = MOCK_TIME,
                        mimeType = "application/pdf",
                    )
                }
            }
        }
        val accessToken = GoogleOAuthMocks.token()
            .enqueue()
            .persist(testData.fry)
        GoogleDriveApiMocks.mockFindFile(
            fileId = "root-folder-id",
            fileName = "simple-accounting",
            expectedAuthToken = accessToken,
        )
        GoogleDriveApiMocks.mockDownloadFile(
            fileId = "gdrive-delivery-contract-file-id",
            content = documentContent,
            expectedAuthToken = accessToken,
        )

        page.authenticateViaCookie(testData.fry)
        page.openDocumentsOverviewPage {
            pageItems {
                val documentItem = shouldHaveItemSatisfying { it.title == "gdrive-delivery-contract.pdf" }
                page.downloadBytes {
                    documentItem.clickLastColumnAction("Download")
                }.shouldBe(documentContent)
            }
        }
    }

    @Test
    fun `should delete document from Google Drive`(page: Page) {
        val testData = preconditions {
            object {
                val fry = platformUser(userName = "Fry", documentsStorage = "google-drive").also {
                    save(
                        GoogleDriveStorageIntegration(
                            userId = it.id!!,
                            folderId = "root-folder-id",
                        )
                    )
                }
                val document = document(
                    workspace = workspace(owner = fry),
                    name = "gdrive-deletable-contract.pdf",
                    storageId = "google-drive",
                    storageLocation = "gdrive-deletable-contract-file-id",
                    timeUploaded = MOCK_TIME,
                    mimeType = "application/pdf",
                )
            }
        }
        val accessToken = GoogleOAuthMocks.token()
            .enqueue()
            .persist(testData.fry)
        GoogleDriveApiMocks.mockFindFile(
            fileId = "root-folder-id",
            fileName = "simple-accounting",
            expectedAuthToken = accessToken,
        )
        GoogleDriveApiMocks.mockDeleteFile(
            fileId = "gdrive-deletable-contract-file-id",
            expectedAuthToken = accessToken,
        )

        page.authenticateViaCookie(testData.fry)
        page.openDocumentsOverviewPage {
            pageItems {
                shouldHaveItemSatisfying { it.title == "gdrive-deletable-contract.pdf" }
                    .clickActionMenuItem("Delete")
            }
            confirmDocumentDeletion()
            pageItems.shouldHaveTitles()
        }

        aggregateTemplate.findById(testData.document.id!!, Document::class.java).shouldBeNull()
        GoogleDriveApiMocks.verifyDeleteFileRequest("gdrive-deletable-contract-file-id")
    }

    @Test
    fun `should navigate to edit expense page from usage link`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry().also {
                    val workspace = workspace(owner = it)
                    val doc = document(
                        workspace = workspace,
                        name = "Expense Receipt.pdf",
                    )
                    expense(
                        workspace = workspace,
                        title = "Slurm supplies",
                        attachments = setOf(doc),
                    )
                }
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.openDocumentsOverviewPage {
            pageItems {
                shouldHaveTitles("Expense Receipt.pdf")
                staticItems[0].clickActionMenuItem("Navigate to Slurm supplies")
            }
        }

        page.shouldBeEditExpensePage {
            title {
                input.shouldHaveValue("Slurm supplies")
            }
        }
    }

    @Test
    fun `should navigate to edit income page from usage link`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry().also {
                    val workspace = workspace(owner = it)
                    val doc = document(
                        workspace = workspace,
                        name = "Income Receipt.pdf",
                    )
                    income(
                        workspace = workspace,
                        title = "Delivery commission",
                        attachments = setOf(doc),
                    )
                }
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.openDocumentsOverviewPage {
            pageItems {
                shouldHaveTitles("Income Receipt.pdf")
                staticItems[0].clickActionMenuItem("Navigate to Delivery commission")
            }
        }

        page.shouldBeEditIncomePage {
            title {
                input.shouldHaveValue("Delivery commission")
            }
        }
    }

    @Test
    fun `should navigate to edit invoice page from usage link`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry().also {
                    val workspace = workspace(owner = it)
                    val doc = document(
                        workspace = workspace,
                        name = "Invoice Attachment.pdf",
                    )
                    val customer = customer(workspace = workspace)
                    invoice(
                        customer = customer,
                        title = "Delivery to Omicron Persei 8",
                        attachments = setOf(doc),
                    )
                }
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.openDocumentsOverviewPage {
            pageItems {
                shouldHaveTitles("Invoice Attachment.pdf")
                staticItems[0].clickActionMenuItem("Navigate to Delivery to Omicron Persei 8")
            }
        }

        page.shouldBeEditInvoicePage {
            title {
                input.shouldHaveValue("Delivery to Omicron Persei 8")
            }
        }
    }

    @Test
    fun `should navigate to edit income tax payment page from usage link`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry().also {
                    val workspace = workspace(owner = it)
                    val doc = document(
                        workspace = workspace,
                        name = "Tax Payment Receipt.pdf",
                    )
                    incomeTaxPayment(
                        workspace = workspace,
                        title = "Q1 Corporate Tax",
                        attachments = setOf(doc),
                    )
                }
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.openDocumentsOverviewPage {
            pageItems {
                shouldHaveTitles("Tax Payment Receipt.pdf")
                staticItems[0].clickActionMenuItem("Navigate to Q1 Corporate Tax")
            }
        }

        page.shouldBeEditIncomeTaxPaymentPage {
            title {
                input.shouldHaveValue("Q1 Corporate Tax")
            }
        }
    }

    @Test
    fun `should filter documents by free search text`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry().also {
                    val workspace = workspace(owner = it)
                    document(
                        workspace = workspace,
                        name = "Slurm delivery manifest.pdf",
                        createdAt = MOCK_TIME.plusSeconds(1),
                    )
                    val usageTitleDocument = document(
                        workspace = workspace,
                        name = "Delivery attachment.pdf",
                        createdAt = MOCK_TIME.plusSeconds(2),
                    )
                    expense(
                        workspace = workspace,
                        title = "Robot oil invoice",
                        attachments = setOf(usageTitleDocument),
                    )
                    document(
                        workspace = workspace,
                        name = "Planet Express permit.pdf",
                        createdAt = MOCK_TIME.plusSeconds(3),
                    )
                }
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.openDocumentsOverviewPage {
            pageItems.shouldHaveTitles(
                "Planet Express permit.pdf",
                "Delivery attachment.pdf",
                "Slurm delivery manifest.pdf",
            )

            filters.addTextFilter("File name or usage title", "slurm")
            pageItems.shouldHaveTitles("Slurm delivery manifest.pdf")

            filters.addTextFilter("File name or usage title", "robot")
            pageItems.shouldHaveTitles("Delivery attachment.pdf")
        }
    }

    @Test
    fun `should filter documents by usage with all dropdown options`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry().also {
                    val workspace = workspace(owner = it)
                    val expenseDocument = document(workspace = workspace, name = "Expense usage.pdf")
                    expense(workspace = workspace, title = "Slurm supplies", attachments = setOf(expenseDocument))

                    val incomeDocument = document(workspace = workspace, name = "Income usage.pdf")
                    income(workspace = workspace, title = "Delivery commission", attachments = setOf(incomeDocument))

                    val invoiceDocument = document(workspace = workspace, name = "Invoice usage.pdf")
                    invoice(
                        customer = customer(workspace = workspace),
                        title = "Delivery to Mars",
                        attachments = setOf(invoiceDocument),
                    )

                    val taxDocument = document(workspace = workspace, name = "Tax usage.pdf")
                    incomeTaxPayment(workspace = workspace, title = "Q1 Corporate Tax", attachments = setOf(taxDocument))

                    val standaloneDocumentFile = document(workspace = workspace, name = "Standalone usage.pdf")
                    standaloneDocument(
                        workspace = workspace,
                        document = standaloneDocumentFile,
                        title = "Planet Express license",
                    )

                    document(workspace = workspace, name = "Unused document.pdf")
                }
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.openDocumentsOverviewPage {
            filters.shouldHaveSelectOptions(
                "Usage",
                "Expense",
                "Income",
                "Invoice",
                "Income Tax Payment",
                "Standalone Document",
                "Unused",
            )

            filters.addSelectFilter("Usage", "Expense")
            pageItems.shouldHaveTitles("Expense usage.pdf")

            filters.clearAll().addSelectFilter("Usage", "Income")
            pageItems.shouldHaveTitles("Income usage.pdf")

            filters.clearAll().addSelectFilter("Usage", "Invoice")
            pageItems.shouldHaveTitles("Invoice usage.pdf")

            filters.clearAll().addSelectFilter("Usage", "Income Tax Payment")
            pageItems.shouldHaveTitles("Tax usage.pdf")

            filters.clearAll().addSelectFilter("Usage", "Standalone Document")
            pageItems.shouldHaveTitles("Standalone usage.pdf")

            filters.clearAll().addSelectFilter("Usage", "Unused")
            pageItems.shouldHaveTitles("Unused document.pdf")
        }
    }

    @Test
    fun `should filter documents by storage with all dropdown options`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry().also {
                    val workspace = workspace(owner = it)
                    document(
                        workspace = workspace,
                        name = "Google Drive storage.pdf",
                        storageId = "google-drive",
                    )
                    document(
                        workspace = workspace,
                        name = "Internal System storage.pdf",
                        storageId = "local-fs",
                    )
                    document(
                        workspace = workspace,
                        name = "Unknown storage.pdf",
                        storageId = "noop",
                    )
                }
            }
        }

        page.authenticateViaCookie(testData.fry)
        page.openDocumentsOverviewPage {
            filters.shouldHaveSelectOptions("Storage", "Google Drive", "Internal System", "Unknown")

            filters.addSelectFilter("Storage", "Google Drive")
            pageItems.shouldHaveTitles("Google Drive storage.pdf")

            filters.clearAll().addSelectFilter("Storage", "Internal System")
            pageItems.shouldHaveTitles("Internal System storage.pdf")

            filters.clearAll().addSelectFilter("Storage", "Unknown")
            pageItems.shouldHaveTitles("Unknown storage.pdf")
        }
    }

    @Test
    fun `should support pagination`(page: Page) {
        val testData = preconditions {
            object {
                val fry = fry().also {
                    val workspace = workspace(owner = it)
                    (1..15).forEach { index ->
                        document(
                            workspace = workspace,
                            name = "Document $index.pdf",
                            createdAt = MOCK_TIME.plusSeconds(index.toLong()),
                        )
                    }
                }
            }
        }

        val firstPageDocuments = (15 downTo 6).map { "Document $it.pdf" }
        val secondPageDocuments = (5 downTo 1).map { "Document $it.pdf" }

        page.authenticateViaCookie(testData.fry)
        page.navigate("/documents")
        page.shouldBeDocumentsOverviewPage {
            pageItems {
                shouldHaveTitles(firstPageDocuments)
                paginator {
                    shouldHaveActivePage(1)
                    shouldHaveTotalPages(2)
                    next()
                }
                shouldHaveTitles(secondPageDocuments)
                paginator {
                    shouldHaveActivePage(2)
                    shouldHaveTotalPages(2)
                    previous()
                }
                shouldHaveTitles(firstPageDocuments)
                paginator {
                    shouldHaveActivePage(1)
                    shouldHaveTotalPages(2)
                }
            }
        }
    }
}
