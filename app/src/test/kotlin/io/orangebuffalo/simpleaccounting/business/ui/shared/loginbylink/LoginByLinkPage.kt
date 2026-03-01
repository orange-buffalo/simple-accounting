package io.orangebuffalo.simpleaccounting.business.ui.shared.loginbylink

import com.microsoft.playwright.Page
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaPageBase
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.SaStatusLabel.Companion.statusLabel
import io.orangebuffalo.simpleaccounting.tests.infra.utils.navigateAndDisableAnimations

class LoginByLinkPage private constructor(page: Page) :
    SaPageBase(page, ".sa-page-without-side-menu__container") {

    val statusMessage = components.statusLabel()

    companion object {
        fun Page.openLoginByLinkPage(token: String, spec: LoginByLinkPage.() -> Unit) {
            LoginByLinkPage(navigateAndDisableAnimations("/login-by-link/$token")).spec()
        }

        fun Page.shouldBeLoginByLinkPage(spec: LoginByLinkPage.() -> Unit) {
            LoginByLinkPage(this).spec()
        }
    }
}
