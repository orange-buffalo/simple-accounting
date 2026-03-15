package io.orangebuffalo.simpleaccounting.business.ui.user.profile

import com.microsoft.playwright.Page
import io.kotest.matchers.shouldBe
import io.orangebuffalo.kotestplaywrightassertions.shouldBeHidden
import io.orangebuffalo.simpleaccounting.business.documents.storage.local.LocalFileSystemDocumentsStorageProperties
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.shared.pages.MyProfilePage.Companion.openMyProfilePage
import io.orangebuffalo.simpleaccounting.business.ui.shared.pages.MyProfilePage.DocumentStorageSection.LocalStorageSettings
import io.orangebuffalo.simpleaccounting.business.ui.shared.pages.MyProfilePage.DocumentStorageSection.StorageSubSection
import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import io.orangebuffalo.simpleaccounting.tests.infra.utils.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean

/**
 * Tests Local document storage integration on My Profile page when local storage is enabled by administrator.
 * See also:
 * - [UserProfileLocalDocumentStorageFullStackTest] for local storage when disabled
 * - [UserProfileGoogleDriveDocumentStorageFullStackTest] for Google Drive storage integration
 */
class UserProfileLocalDocumentStorageEnabledFullStackTest : SaFullStackTestBase() {

    @MockitoSpyBean
    private lateinit var localFsProperties: LocalFileSystemDocumentsStorageProperties

    @BeforeEach
    fun enableLocalFs() {
        whenever(localFsProperties.enabled).thenReturn(true)
    }

    @Test
    fun `should show local storage with 'Use for uploads' action when enabled but not selected`(
        page: Page
    ) = page.onLocalStorageSection(preconditions.scruffy) {
        withHint("Local storage should show 'Use for uploads' action") {
            shouldHaveUseForUploadsAction()
        }

        withHint("Should not show disabled explanation") {
            infoMessage.shouldBeHidden()
        }
    }

    @Test
    fun `should show local storage with 'Used for uploads' status when selected`(
        page: Page
    ) = page.onLocalStorageSection(preconditions.bender) {
        withHint("Local storage should show 'Used for uploads' status") {
            shouldHaveUsedForUploadsStatus()
        }
    }

    @Test
    fun `should enable local storage for uploads when clicking action link`(
        page: Page
    ) = page.onLocalStorageSection(preconditions.scruffy) {
        withHint("Local storage should show 'Use for uploads' action") {
            shouldHaveUseForUploadsAction()
        }

        clickUseForUploads()

        shouldEventually("Should show 'Used for uploads' status after clicking") {
            shouldHaveUsedForUploadsStatus()
        }

        withHint("Should save settings") {
            aggregateTemplate.findSingle<PlatformUser>(preconditions.scruffy.id!!)
                .documentsStorage.shouldBe("local-fs")
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

            val bender = platformUser(
                userName = "bender",
                documentsStorage = "local-fs",
            ).withWorkspace()
        }
    }
}
