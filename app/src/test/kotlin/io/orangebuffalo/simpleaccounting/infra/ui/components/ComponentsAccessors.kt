package io.orangebuffalo.simpleaccounting.infra.ui.components

import com.microsoft.playwright.Page

class ComponentsAccessors<T : SaPageBase<T>>(val page: Page, val owner: T)
