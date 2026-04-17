package io.orangebuffalo.simpleaccounting.business.ui.admin.usermanagement

import com.microsoft.playwright.Page
import io.kotest.matchers.collections.shouldContainAll
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.admin.usermanagement.UsersOverviewPage.Companion.openUsersOverviewPage
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaActionLink
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaIconType
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaOverviewItem.Companion.primaryAttribute
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaOverviewItemData
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.shouldHaveTitles
import io.orangebuffalo.simpleaccounting.tests.infra.utils.MOCK_TIME
import org.junit.jupiter.api.Test

class UsersOverviewFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should provide users overview`(page: Page) {
        val preconditions = setupOverviewPreconditions()
        page.authenticateViaCookie(preconditions.farnsworth)
        page.openUsersOverviewPage {
            pageItems {
                shouldHaveExactData(
                    SaOverviewItemData(
                        title = "C User",
                        primaryAttributes = listOf(
                            primaryAttribute(SaIconType.REGULAR_USER, "User"),
                            primaryAttribute(SaIconType.INACTIVE_USER, "Not yet activated"),
                        ),
                        lastColumnContent = SaActionLink.editActionLinkValue(),
                        hasDetails = false,
                    ),
                    SaOverviewItemData(
                        title = "B user",
                        primaryAttributes = listOf(
                            primaryAttribute(SaIconType.ADMIN_USER, "Admin user"),
                            primaryAttribute(SaIconType.INACTIVE_USER, "Not yet activated"),
                        ),
                        lastColumnContent = SaActionLink.editActionLinkValue(),
                        hasDetails = false,
                    ),
                    SaOverviewItemData(
                        title = "aUser",
                        primaryAttributes = listOf(
                            primaryAttribute(SaIconType.REGULAR_USER, "User"),
                            primaryAttribute(SaIconType.ACTIVE_USER, "Active"),
                        ),
                        lastColumnContent = SaActionLink.editActionLinkValue(),
                        hasDetails = false,
                    ),
                    SaOverviewItemData(
                        title = "Farnsworth",
                        primaryAttributes = listOf(
                            primaryAttribute(SaIconType.ADMIN_USER, "Admin user"),
                            primaryAttribute(SaIconType.ACTIVE_USER, "Active"),
                        ),
                        lastColumnContent = SaActionLink.editActionLinkValue(),
                        hasDetails = false,
                    ),
                )
            }
        }
    }

    @Test
    fun `should support pagination`(page: Page) {
        val preconditions = setupPaginationPreconditions()
        page.authenticateViaCookie(preconditions.farnsworth)
        // users 15..6 (newest first) on page 1
        val firstPageUsers = (15 downTo 6).map { "user $it" }
        // users 5..1 and Farnsworth on page 2
        val secondPageUsers = (5 downTo 1).map { "user $it" } + "Farnsworth"
        page.openUsersOverviewPage {
            pageItems {
                shouldHaveTitles(firstPageUsers)
                paginator {
                    shouldHaveActivePage(1)
                    shouldHaveTotalPages(2)
                    next()
                    shouldHaveActivePage(2)
                    shouldHaveTotalPages(2)
                }
                shouldHaveTitles(secondPageUsers)
                paginator {
                    previous()
                    shouldHaveActivePage(1)
                    shouldHaveTotalPages(2)
                }
                shouldHaveTitles(firstPageUsers)
            }
        }
    }

    @Test
    fun `should support filtering`(page: Page) {
        val preconditions = setupFilteringPreconditions()
        page.authenticateViaCookie(preconditions.farnsworth)
        page.openUsersOverviewPage {
            pageItems {
                // ensure all targeted items visible by default
                shouldHaveDataSatisfying { data ->
                    data.map { it.title }.shouldContainAll(
                        "aBcDef", "abcdef", "ABCDEF", "qwerty"
                    )
                }
                // ensure we update paginator as well
                paginator {
                    shouldHaveActivePage(1)
                    shouldHaveTotalPages(2)
                }
            }
            filterInput { fill("cd") }
            pageItems {
                // order is by createdAt desc: ABCDEF (newest), abcdef, aBcDef (oldest)
                shouldHaveTitles("ABCDEF", "abcdef", "aBcDef")
                paginator {
                    shouldHaveActivePage(1)
                    shouldHaveTotalPages(1)
                }
            }
        }
    }

    private fun setupOverviewPreconditions() = preconditions {
        object {
            val farnsworth = farnsworth()

            init {
                platformUser(userName = "aUser", isAdmin = false, activated = true, createdAt = MOCK_TIME.plusSeconds(1))
                platformUser(userName = "B user", isAdmin = true, activated = false, createdAt = MOCK_TIME.plusSeconds(2))
                platformUser(userName = "C User", isAdmin = false, activated = false, createdAt = MOCK_TIME.plusSeconds(3))
            }
        }
    }

    private fun setupPaginationPreconditions() = preconditions {
        object {
            var farnsworth = farnsworth()

            init {
                (1..15).forEachIndexed { index, i ->
                    platformUser(userName = "user $i", createdAt = MOCK_TIME.plusSeconds(index.toLong() + 1))
                }
            }
        }
    }

    private fun setupFilteringPreconditions() = preconditions {
        object {
            var farnsworth = farnsworth()

            init {
                // extra users to enable pagination - must have older timestamps than target users
                (1..10).forEachIndexed { index, i ->
                    platformUser(userName = "user $i", createdAt = MOCK_TIME.plusSeconds(index.toLong() + 1))
                }
                // target users with newer timestamps so they appear on the first page (createdAt DESC)
                platformUser(userName = "aBcDef", createdAt = MOCK_TIME.plusSeconds(11))
                platformUser(userName = "abcdef", createdAt = MOCK_TIME.plusSeconds(12))
                platformUser(userName = "ABCDEF", createdAt = MOCK_TIME.plusSeconds(13))
                platformUser(userName = "qwerty", createdAt = MOCK_TIME.plusSeconds(14))
            }
        }
    }
}
