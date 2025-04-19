package io.orangebuffalo.simpleaccounting.tests.ui.user

import com.microsoft.playwright.Page
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.orangebuffalo.simpleaccounting.business.users.PlatformUser
import io.orangebuffalo.simpleaccounting.tests.infra.SimpleAccountingFullStackTest
import io.orangebuffalo.simpleaccounting.tests.infra.database.PreconditionsFactory
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findSingle
import io.orangebuffalo.simpleaccounting.tests.infra.utils.withBlockedApiResponse
import io.orangebuffalo.simpleaccounting.tests.ui.shared.components.shouldHaveSideMenu
import io.orangebuffalo.simpleaccounting.tests.ui.shared.pages.MyProfilePage
import io.orangebuffalo.simpleaccounting.tests.ui.shared.pages.OAuthAuthorizationPopup.Companion.shouldHaveAuthorizationPopupOpenBy
import io.orangebuffalo.simpleaccounting.tests.ui.shared.pages.loginAs
import io.orangebuffalo.simpleaccounting.tests.ui.shared.pages.shouldBeMyProfilePage
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jdbc.core.JdbcAggregateTemplate

@SimpleAccountingFullStackTest
class UserProfileGoogleDriveDocumentStorageFullStackTest(
    preconditionsFactory: PreconditionsFactory,
    @Autowired private val aggregateTemplate: JdbcAggregateTemplate,
) {
    /**
     * This test combines multiple cases (e.g. for auth) in order to not couple
     * to any implementation of the OAuth flow, and reduce coupling to
     * other implementation details.
     */
    @Test
    @RepeatedTest(1000)
    fun `should support Google Drive storage`(page: Page) = page.open(preconditions.scruffy) {
        shouldHaveDocumentsStorageSectionVisible {
            shouldHaveGoogleDriveConfigVisible {
                withClue("Google Drive should be turned off") {
                    switch.shouldBeSwitchedOff()
                    settings.shouldBeHidden()
                }

                withClue("Should show loading indicator when turned on") {
                    page.withBlockedApiResponse(
                        "**/status",
                        initiator = {
                            switch.toggle()
                            settings.status.shouldBeRegular("Verifying integration status...")
                        },
                        blockedRequestSpec = {
                            assertRendering("profile/documents-storage/google/loading-status")
                        }
                    )
                }

                withClue("Should update database state") {
                    aggregateTemplate.findSingle<PlatformUser>(preconditions.scruffy.id!!)
                        .documentsStorage.shouldBe("google-drive")
                }

                withClue("Should have unauthorized status until configured") {
                    settings {
                        shouldBeVisible()
                        status.shouldBePending("Authorization required")
                    }
                    assertRendering("profile/documents-storage/google/authorization-required")
                }

                val oauthPopup = page.shouldHaveAuthorizationPopupOpenBy {
                    settings.startAuthorizationButton.click()
                }
                println()

            }
        }
    }

    private fun Page.open(user: PlatformUser, spec: MyProfilePage.() -> Unit) {
        loginAs(user)
        shouldHaveSideMenu().clickMyProfile()
        spec(shouldBeMyProfilePage())
    }

    private val preconditions by preconditionsFactory {
        object {
            val scruffy = platformUser(
                userName = "scruffy",
                documentsStorage = null,
            )

            init {
                workspace(owner = scruffy)
            }
        }
    }
}
