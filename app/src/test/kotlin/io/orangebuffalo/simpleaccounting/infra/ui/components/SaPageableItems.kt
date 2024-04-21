package io.orangebuffalo.simpleaccounting.infra.ui.components

import com.microsoft.playwright.Locator
import io.orangebuffalo.simpleaccounting.infra.ui.components.Paginator.Companion.twoSyncedPaginators

class SaPageableItems<I, P: Any> private constructor(
    private val container: Locator,
    parent: P,
    val paginator: Paginator,
    private val itemFactory: (container: Locator) -> I
) : UiComponent<P, SaPageableItems<I, P>>(parent) {

    private val items: List<I>
        get() = container.locator(".sa-pageable-items__item")
            .all()
            .map { itemFactory(it) }

    /**
     * Waits for the items to satisfy the given spec. Re-evaluates
     * current items and invokes spec until the condition is satisfied.
     * Spec must throw assertion error if the condition is not satisfied.
     */
    fun shouldSatisfy(spec: (items: List<I>) -> Unit) = io.orangebuffalo.simpleaccounting.infra.utils.shouldSatisfy {
        spec(items)
    }

    companion object {
        fun <T : SaPageBase<T>, I> ComponentsAccessors<T>.pageableItems(
            itemFactory: (container: Locator) -> I
        ): SaPageableItems<I, T> {
            val container = page.locator(".sa-pageable-items")
            return SaPageableItems(
                container = container,
                parent = this.owner,
                paginator = this.twoSyncedPaginators(container),
                itemFactory = itemFactory
            )
        }
    }
}
