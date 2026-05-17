package io.orangebuffalo.simpleaccounting.business.ui.user.documents

import com.microsoft.playwright.Page
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveText
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.shared.pages.MyProfilePage.Companion.openMyProfilePage
import io.orangebuffalo.simpleaccounting.business.ui.user.documents.DocumentsMigrationPage.Companion.shouldBeDocumentsMigrationPage
import io.orangebuffalo.simpleaccounting.tests.infra.utils.withHint
import io.orangebuffalo.simpleaccounting.tests.infra.utils.withBlockedGqlApiResponse
import org.junit.jupiter.api.Test

class DocumentsMigrationFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should navigate from profile storage notice and show documents migration summary`(page: Page) {
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
                        "documentsMigrationStorageStatistics",
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

        withHint("Should show total documents outside of upload storage") {
            page.shouldBeDocumentsMigrationPage {
                shouldHaveDescription(
                    "3 documents are outside of the upload storage. " +
                        "This page can help migrate them to the current storage."
                )
                reportRendering("documents-migration.loaded")
            }
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
