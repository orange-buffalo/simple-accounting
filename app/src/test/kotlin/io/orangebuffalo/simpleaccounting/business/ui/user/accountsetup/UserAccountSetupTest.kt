package io.orangebuffalo.simpleaccounting.business.ui.user.accountsetup

import com.microsoft.playwright.Page
import com.microsoft.playwright.Route
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.booleans.shouldBeTrue
import io.orangebuffalo.simpleaccounting.business.ui.SaFullStackTestBase
import io.orangebuffalo.simpleaccounting.business.ui.shared.login.LoginPage.Companion.loginAs
import io.orangebuffalo.simpleaccounting.business.ui.user.accountsetup.AccountSetupPage.Companion.shouldBeAccountSetupPage
import io.orangebuffalo.simpleaccounting.business.ui.user.dashboard.DashboardPage.Companion.shouldBeDashboardPage
import io.orangebuffalo.simpleaccounting.business.workspaces.Workspace
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.shouldHaveSideMenu
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.shouldHaveSideMenuHidden
import io.orangebuffalo.simpleaccounting.tests.infra.utils.findSingle
import io.orangebuffalo.simpleaccounting.tests.infra.utils.withBlockedGqlApiResponse
import io.orangebuffalo.simpleaccounting.tests.infra.utils.UI_ASSERTIONS_TIMEOUT_MS
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import java.time.Duration

class UserAccountSetupTest : SaFullStackTestBase() {

    @Test
    fun `should setup new account successfully`(page: Page) {
        page.loginAs(preconditions.fry)
        page.shouldHaveSideMenuHidden()
        page.shouldBeAccountSetupPage {
            workspaceName { input.fill("Planet Express") }
            defaultCurrency {
                input.shouldHaveValue("AUD")
            }
            residency {
                input.selectOption("Australia")
            }
            completeSetupButton { click() }
        }
        page.shouldBeDashboardPage()
        page.shouldHaveSideMenu()
            .shouldHaveWorkspaceName("Planet Express")

        aggregateTemplate.findSingle<Workspace>().should { workspace ->
            workspace.name.shouldBe("Planet Express")
            workspace.residency.shouldBe("AU")
            workspace.defaultCurrency.shouldBe("AUD")
            workspace.ownerId.shouldBe(preconditions.fry.id)
        }
    }

    @Test
    fun `should reset residency and restrict countries when currency changes`(page: Page) {
        page.loginAs(preconditions.fry)
        page.shouldBeAccountSetupPage {
            residency { input.selectOption("Australia") }
            defaultCurrency { input.fill("") }
            residency {
                input.shouldBeDisabled()
                input.shouldBeEmpty()
                input.shouldHavePlaceholder("Select a currency first")
            }
            defaultCurrency { input.fill("USD") }
            residency {
                input.shouldBeEmpty()
                input.shouldHaveOptions(
                    "American Samoa", "British Indian Ocean Territory", "British Virgin Islands", "Caribbean Netherlands", "Ecuador",
                    "Guam", "Haiti", "Marshall Islands", "Micronesia", "Northern Mariana Islands", "Palau", "Panama",
                    "Puerto Rico", "Timor-Leste", "Turks & Caicos Islands", "U.S. Outlying Islands",
                    "U.S. Virgin Islands", "United States",
                )
                input.selectOption("United States")
            }
            defaultCurrency { input.fill("XXX") }
            residency {
                input.shouldBeDisabled()
                input.shouldBeEmpty()
                input.shouldHavePlaceholder("No countries available for this currency")
            }
        }
    }

    @Test
    fun `should ignore countries loaded for a previous currency`(page: Page) {
        page.loginAs(preconditions.fry)
        page.shouldBeAccountSetupPage {
            residency { input.selectOption("Australia") }
            var earlierRequestFinished = false
            page.onRequestFinished { request ->
                if (request.postData()?.contains("\"currency\":\"INR\"") == true) {
                    earlierRequestFinished = true
                }
            }
            page.withBlockedGqlApiResponse(
                "countriesForResidency",
                initiator = {
                    defaultCurrency { input.fill("INR") }
                    defaultCurrency.shouldBeVisible()
                },
                blockedRequestSpec = {
                    residency {
                        input.shouldBeDisabled()
                        input.shouldHavePlaceholder("Loading countries...")
                    }
                    page.context().unroute("/api/graphql")
                    residency { input.shouldBeEmpty() }
                    defaultCurrency { input.fill("ZAR") }
                    residency {
                        input.shouldHaveOptions("Lesotho", "Namibia", "South Africa")
                        input.selectOption("South Africa")
                    }
                },
            )
            await().atMost(Duration.ofMillis(UI_ASSERTIONS_TIMEOUT_MS.toLong())).untilAsserted {
                residency { input.shouldHaveSelectedValue("South Africa") }
                earlierRequestFinished.shouldBeTrue()
            }
            residency {
                input.shouldHaveOptions("Lesotho", "Namibia", "South Africa")
                input.shouldHaveSelectedValue("South Africa")
            }
        }
    }

    @Test
    fun `should show country loading failure and recover on currency change`(page: Page) {
        page.loginAs(preconditions.fry)
        page.shouldBeAccountSetupPage {
            residency { input.selectOption("Australia") }
            page.context().route("/api/graphql") { route ->
                val request = route.request().postData().orEmpty()
                if (request.contains("countriesForResidency") && request.contains("\"currency\":\"INR\"")) {
                    route.fulfill(
                        Route.FulfillOptions().setContentType("application/json")
                            .setBody("""{"errors":[{"message":"Slurm country registry is unavailable"}]}"""),
                    )
                } else {
                    route.resume()
                }
            }
            try {
                defaultCurrency { input.fill("INR") }
                shouldHaveNotifications { error() }
                residency {
                    input.shouldBeDisabled()
                    input.shouldBeEmpty()
                    input.shouldHavePlaceholder("Could not load countries. Refresh the page to try again.")
                }
                reportRendering("account-setup.country-loading-failed")
                defaultCurrency { input.fill("ZAR") }
                residency {
                    input.shouldHaveOptions("Lesotho", "Namibia", "South Africa")
                    input.selectOption("South Africa")
                    input.shouldHaveSelectedValue("South Africa")
                }
            } finally {
                page.context().unroute("/api/graphql")
            }
        }
    }

    /**
     * Verifies integration of API errors with UI. Full set of validation cases is tested on the API level.
     */
    @Test
    fun `should validate inputs`(page: Page) {
        page.loginAs(preconditions.fry)
        page.shouldBeAccountSetupPage {
            workspaceName { input.fill("") }
            defaultCurrency { input.fill("") }
            completeSetupButton { click() }
            shouldHaveNotifications { validationFailed() }
            workspaceName { shouldHaveValidationError("This value is required and should not be blank") }
            defaultCurrency { shouldHaveValidationError("This value is required and should not be blank") }
            residency { shouldHaveValidationError("This value is required and should not be blank") }
            workspaceName { input.fill("x".repeat(256)) }
            defaultCurrency { input.fill("x".repeat(4)) }
            completeSetupButton { click() }
            shouldHaveNotifications { validationFailed() }
            workspaceName { shouldHaveValidationError("The length of this value should be no longer than 255 characters") }
            defaultCurrency { shouldHaveValidationError("The length of this value should be no longer than 3 characters") }
        }
    }

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry()
            // no workspace yet - setup is required
        }
    }
}
