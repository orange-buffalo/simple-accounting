package io.orangebuffalo.simpleaccounting.business.ui.user.profile

import com.microsoft.playwright.Page
import io.kotest.matchers.shouldBe
import io.orangebuffalo.kotestplaywrightassertions.shouldBeHidden
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.kotestplaywrightassertions.shouldHaveText
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.shared.pages.MyProfilePage.Companion.openMyProfilePage
import io.orangebuffalo.simpleaccounting.business.ui.shared.pages.MyProfilePage.DocumentStorageSection.LocalStorageSettings
import io.orangebuffalo.simpleaccounting.business.ui.shared.pages.MyProfilePage.DocumentStorageSection.StorageSubSection
import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import io.orangebuffalo.simpleaccounting.tests.infra.utils.*
import org.junit.jupiter.api.Test

/**
 * Tests Local document storage integration on My Profile page when local storage is disabled by administrator.
 * See also:
 * - [UserProfileFullStackTest] for basic My Profile page rendering
 * - [UserProfileLocalDocumentStorageEnabledFullStackTest] for local storage when enabled
 * - [UserProfileGoogleDriveDocumentStorageFullStackTest] for Google Drive storage integration
 */
class UserProfileLocalDocumentStorageFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should show local storage as not available when system setting is disabled`(
        page: Page
    ) = page.onLocalStorageSection(preconditions.scruffy) {
        withHint("Local storage should show 'Not available' status") {
            shouldHaveNotAvailableStatus()
        }

        withHint("Should show disabled explanation") {
            infoMessage.shouldBeVisible()
            infoMessage.shouldHaveText(
                "Local storage has not been enabled by the system administrator."
            )
        }
    }

    private fun Page.onLocalStorageSection(
        user: PlatformUser,
        spec: StorageSubSection<LocalStorageSettings>.() -> Unit
    ) {
        authenticateViaCookie(user)
        openMyProfilePage {
            shouldHaveDocumentsStorageSectionVisible {
                shouldHaveLocalStorageConfigVisible {
                    spec(this)
                }
            }
        }
    }

    private val preconditions by lazyPreconditions {
        object {
            val scruffy = platformUser(
                userName = "scruffy",
                documentsStorage = null,
            ).withWorkspace()
        }
    }
}
