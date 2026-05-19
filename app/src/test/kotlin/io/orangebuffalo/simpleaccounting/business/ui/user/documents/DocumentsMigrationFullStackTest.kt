package io.orangebuffalo.simpleaccounting.business.ui.user.documents

import com.microsoft.playwright.Page
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveText
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.shared.pages.MyProfilePage.Companion.openMyProfilePage
import io.orangebuffalo.simpleaccounting.business.ui.user.documents.DocumentsMigrationPage.Companion.openDocumentsMigrationPage
import io.orangebuffalo.simpleaccounting.business.ui.user.documents.DocumentsMigrationPage.Companion.shouldBeDocumentsMigrationPage
import io.orangebuffalo.simpleaccounting.tests.infra.ui.TestDocumentsStorage
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import io.orangebuffalo.simpleaccounting.tests.infra.utils.withHint
import io.orangebuffalo.simpleaccounting.tests.infra.utils.withBlockedGqlApiResponse
import org.junit.jupiter.api.Test

class DocumentsMigrationFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should navigate from profile storage notice and show loading state`(page: Page) {
        page.authenticateViaCookie(preconditions.fry)

        page.openMyProfilePage {
            shouldHaveDocumentsStorageSectionVisible {
                shouldHaveGoogleDriveConfigVisible {
                    infoMessage.shouldBeVisible()
                    infoMessage.shouldHaveText(
                        "3 documents have been uploaded with Google Drive. " +
                            "You need to keep the authorization active in order to download them. " +
                            "Navigate to the Documents Migration page to move the documents to the current storage."
                    )
                    reportRendering("profile.documents-storage.migration-link")

                    page.withBlockedGqlApiResponse(
                        "documentsMigrationPageData",
                        initiator = {
                            clickMigrationLink()
                        },
                        blockedRequestSpec = {
                            page.shouldBeDocumentsMigrationPage {
                                shouldHaveLoadingIndicatorVisible()
                                reportRendering("documents-migration.loading")
                            }
                        }
                    )
                }
            }
        }

        withHint("Should show storage unavailable message when used storage is not active") {
            page.shouldBeDocumentsMigrationPage {
                shouldHaveDescription(
                    "One of the required storages is not available. " +
                        "Please configure it on your profile page."
                )
                reportRendering("documents-migration.storage-unavailable")
            }
        }
    }

    @Test
    fun `should show no migration required when all documents are in upload storage`(page: Page) {
        val testData = preconditions {
            object {
                val bender = platformUser(userName = "Bender", documentsStorage = TestDocumentsStorage.STORAGE_ID).also {
                    val workspace = workspace(owner = it)
                    document(workspace = workspace, storageId = TestDocumentsStorage.STORAGE_ID)
                    document(workspace = workspace, storageId = TestDocumentsStorage.STORAGE_ID)
                }
            }
        }

        page.authenticateViaCookie(testData.bender)
        page.openDocumentsMigrationPage {
            shouldHaveDescription("All your documents are in the upload storage, no migration is required.")
            reportRendering("documents-migration.no-migration-required")
        }
    }

    @Test
    fun `should show active migration progress when latest migration is not complete`(page: Page) {
        val testData = preconditions {
            object {
                val hermes = platformUser(userName = "Hermes", documentsStorage = TestDocumentsStorage.STORAGE_ID).also {
                    val workspace = workspace(owner = it)
                    val slurm = document(workspace = workspace, storageId = "noop", name = "Slurm receipt")
                    val robotOil = document(workspace = workspace, storageId = "noop", name = "Robot oil receipt")
                    val spaceshipParts = document(workspace = workspace, storageId = "noop", name = "Spaceship parts receipt")
                    documentsMigration(
                        user = it,
                        documentsToMigrate = setOf(slurm, robotOil, spaceshipParts),
                        migratedDocumentsCount = 1,
                        createdAt = MOCK_TIME.plusSeconds(100),
                    )
                }
            }
        }

        page.authenticateViaCookie(testData.hermes)
        page.openDocumentsMigrationPage {
            shouldHaveDescription(
                "We are currently migrating your documents, please avoid editing existing entries " +
                    "until we complete the migration."
            )
            shouldHaveProgress("1/3 migrated")
            reportRendering("documents-migration.in-progress")
        }
    }

    @Test
    fun `should show start migration action and render returned migration after start`(page: Page) {
        val testData = preconditions {
            object {
                val leela = platformUser(userName = "Leela", documentsStorage = TestDocumentsStorage.STORAGE_ID).also {
                    val workspace = workspace(owner = it)
                    document(workspace = workspace, storageId = "noop", name = "Moon delivery receipt")
                    document(workspace = workspace, storageId = "noop", name = "Mars delivery receipt")
                }
            }
        }

        page.authenticateViaCookie(testData.leela)
        page.openDocumentsMigrationPage {
            shouldHaveDescription(
                "2 documents are outside of the upload storage. " +
                    "We can migrate all the documents to the current upload storage. " +
                    "To start the process, use the button below. " +
                    "Please note it is not reversible and cannot be interrupted until finished."
            )
            shouldHaveStartMigrationButton()
            reportRendering("documents-migration.ready-to-start")
            clickStartMigration()
        }

        page.shouldBeDocumentsMigrationPage {
            shouldHaveDescription(
                "We are currently migrating your documents, please avoid editing existing entries " +
                    "until we complete the migration."
            )
            shouldHaveProgress("0/2 migrated")
            reportRendering("documents-migration.started")
        }
    }

    @Test
    fun `should show storage unavailable when upload storage is not assigned`(page: Page) {
        val testData = preconditions {
            object {
                val amy = platformUser(userName = "Amy", documentsStorage = null).withWorkspace()
            }
        }

        page.authenticateViaCookie(testData.amy)
        page.openDocumentsMigrationPage {
            shouldHaveDescription(
                "One of the required storages is not available. " +
                    "Please configure it on your profile page."
            )
            reportRendering("documents-migration.no-upload-storage")
        }
    }

    private val preconditions by lazyPreconditions {
        object {
            val fry = platformUser(userName = "Fry", documentsStorage = "local-fs").also {
                val workspace = workspace(owner = it)
                document(workspace = workspace, storageId = "google-drive")
                document(workspace = workspace, storageId = "google-drive")
                document(workspace = workspace, storageId = "google-drive")
                document(workspace = workspace, storageId = "local-fs")
            }
        }
    }
}
