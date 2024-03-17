package io.orangebuffalo.simpleaccounting.infra.ui.components

import com.microsoft.playwright.Page

/**
 * The purpose of this intermediary is to reduce the amount of code
 * necessary for declaring components in the page objects. We can hide dragging
 * parent reference (and other data if necessary) from the component declaration.
 *
 * It also allows to have declarative factory methods for components that focus on
 * the necessary data and hide the common details.
 */
class ComponentsAccessors<T : SaPageBase<T>>(val page: Page, val owner: T)
