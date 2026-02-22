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
                        title = "aUser",
                        primaryAttributes = listOf(
                            primaryAttribute(SaIconType.REGULAR_USER, "User"),
                            primaryAttribute(SaIconType.ACTIVE_USER, "Active"),
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
                        title = "C User",
                        primaryAttributes = listOf(
                            primaryAttribute(SaIconType.REGULAR_USER, "User"),
                            primaryAttribute(SaIconType.INACTIVE_USER, "Not yet activated"),
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
        val firsPageUsers = listOf(
            "Farnsworth", "user 1", "user 10", "user 11", "user 12",
            "user 13", "user 14", "user 15", "user 2", "user 3"
        )
        page.openUsersOverviewPage {
            pageItems {
                shouldHaveTitles(firsPageUsers)
                paginator {
                    shouldHaveActivePage(1)
                    shouldHaveTotalPages(2)
                    next()
                    shouldHaveActivePage(2)
                    shouldHaveTotalPages(2)
                }
                shouldHaveTitles("user 4", "user 5", "user 6", "user 7", "user 8", "user 9")
                paginator {
                    previous()
                    shouldHaveActivePage(1)
                    shouldHaveTotalPages(2)
                }
                shouldHaveTitles(firsPageUsers)
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
                shouldHaveTitles("aBcDef", "abcdef", "ABCDEF")
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
                platformUser(userName = "aUser", isAdmin = false, activated = true)
                platformUser(userName = "B user", isAdmin = true, activated = false)
                platformUser(userName = "C User", isAdmin = false, activated = false)
            }
        }
    }

    private fun setupPaginationPreconditions() = preconditions {
        object {
            var farnsworth = farnsworth()

            init {
                (1..15).forEach { platformUser(userName = "user $it") }
            }
        }
    }

    private fun setupFilteringPreconditions() = preconditions {
        object {
            var farnsworth = farnsworth()

            init {
                platformUser(userName = "aBcDef")
                platformUser(userName = "abcdef")
                platformUser(userName = "ABCDEF")
                platformUser(userName = "qwerty")
                // some users to enable pagination
                (1..10).forEach { platformUser(userName = "user $it") }
            }
        }
    }
}
