package io.orangebuffalo.simpleaccounting.infra.ui.components

import com.microsoft.playwright.Locator
import org.awaitility.Awaitility.await

class SaPageableItems<I, P> private constructor(
    private val container: Locator,
    private val parent: P,
    private val itemFactory: (container: Locator) -> I
) {

    private val items: List<I>
        get() = container.locator(".sa-pageable-items__item")
            .all()
            .map { itemFactory(it) }

    /**
     * Waits for the items to satisfy the given spec. Re-evaluates
     * current items and invokes spec until the condition is satisfied.
     * Spec must throw assertion error if the condition is not satisfied.
     */
    operator fun invoke(spec: (items: List<I>) -> Unit): P {
        await().untilAsserted {
            spec(items)
        }
        return parent
    }

    companion object {
        fun <T : SaPageBase<T>, I> ComponentsAccessors<T>.pageableItems(itemFactory: (container: Locator) -> I) =
            SaPageableItems(page.locator(".sa-pageable-items"), this.owner, itemFactory)
    }
}
