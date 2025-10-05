package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Page

/**
 * The purpose of this intermediary is to reduce the amount of code
 * necessary for declaring components in the page objects. We can hide dragging
 * common data (like page reference) from the component declaration.
 *
 * It also allows to have declarative factory methods for components that focus on
 * the necessary data and hide the common details.
 */
class ComponentsAccessors(val page: Page)
