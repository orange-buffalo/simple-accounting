package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

/**
 * Base class for UI components.
 * We chose to use a DSL approach for UI components operation to keep the UI
 * tests code structured and well-scoped. Hence, each component can be invoked with
 * a lambda that defines the operations to be performed on the component.
 */
@UiComponentMarker
abstract class UiComponent<P : Any, T : UiComponent<P, T>>(
    protected val parent: P,
) {
    operator fun invoke(action: T.() -> Unit): P {
        self().action()
        return parent
    }

    @Suppress("UNCHECKED_CAST")
    private fun self() = this as T
}

/**
 * Helper marker to restrict the receiver usage in [UiComponent] invocations.
 * Not intended to be used directly.
 */
@DslMarker
annotation class UiComponentMarker
