package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import com.microsoft.playwright.Locator
import io.kotest.matchers.collections.shouldContainExactly
import io.orangebuffalo.kotestplaywrightassertions.shouldBeVisible
import io.orangebuffalo.simpleaccounting.tests.infra.ui.components.Paginator.Companion.twoSyncedPaginators
import io.orangebuffalo.simpleaccounting.tests.infra.utils.injectJsUtils
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldBeSingle
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldSatisfy
import io.orangebuffalo.simpleaccounting.tests.infra.utils.shouldWithClue
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class SaPageableItems<I, D : Any> private constructor(
    private val container: Locator,
    val paginator: Paginator,
    private val itemDataJs: String,
    private val itemDataSerializer: KSerializer<D>,
    private val itemFactory: (container: Locator) -> I,
) : UiComponent<SaPageableItems<I, D>>() {

    private val itemsEls = container.locator(".sa-pageable-items__item")

    /**
     * Warning! Will provide the items as are during the call. User [shouldHaveExactItems] to ensure
     * the items are available in case of dynamic content.
     */
    val staticItems: List<I>
        get() = itemsEls.all().map { itemFactory(it) }

    /**
     * Waits for the items to be the provided values, which are mapped from the items.
     */
    fun <T> shouldHaveExactItems(vararg items: T, mapper: (item: I) -> T) {
        shouldSatisfy("Pageable items should match the expected items") {
            staticItems
                .map(mapper)
                .shouldContainExactly(*items)
        }
    }

    /**
     * Ensures that the item satisfying the predicate is present, and returns it.
     */
    fun shouldHaveItemSatisfying(itemPredicate: (item: I) -> Boolean): I {
        var item: I? = null
        shouldSatisfy("Pageable items should contain an item satisfying the predicate") {
            staticItems
                .filter { itemPredicate(it) }
                .shouldWithClue("Exactly one item expected to satisfy the predicate") {
                    item = shouldBeSingle()
                }
        }
        return item!!
    }

    /**
     * Loader is debounced and requires a tick to finish loading.
     */
    fun finishLoadingWhenTimeMocked() {
        container.page().clock().runFor(1)
    }

    /**
     * Verifies that the loading indicator is visible.
     */
    fun shouldHaveLoadingIndicatorVisible() {
        container.locator(".sa-pageable-items__loader-item").first().shouldBeVisible()
    }

    /**
     * Asserts that the data of the pageable items satisfies the given specification.
     */
    fun shouldHaveDataSatisfying(dataSpec: (data: List<D>) -> Unit) {
        shouldSatisfy("Pageable items data should satisfy the expected specification") {
            val data = getData()
            dataSpec(data)
        }
    }

    /**
     * Asserts that the pageable items have the given data, in exact order.
     */
    fun shouldHaveExactData(vararg expectedData: D) {
        shouldHaveDataSatisfying { data ->
            data.shouldContainExactly(*expectedData)
        }
    }

    private fun getData(): List<D> {
        /* language=javascript */
        val dataJsonString = container.evaluate(
            """
                (container) => {
                    ${injectJsUtils()}
                    const getItemData = ${itemDataJs};
                    return JSON.stringify(Array.from(container.querySelectorAll('.sa-pageable-items__item'))
                        .map(item => getItemData(item)));
                }
                """,
        ) as String
        return Json.decodeFromString(ListSerializer(itemDataSerializer), dataJsonString)
    }

    companion object {
        fun <I, D : Any> ComponentsAccessors.pageableItems(
            /**
             * JavaScript function that takes an item element and returns its data as a JS object.
             * It is used for performance optimization to avoid numerous interactions with the browser,
             * but instead retrieve all the data in one go.
             */
            itemDataJs: String,
            /**
             * Serializer for the item data. The object returned by [itemDataJs] should be compatible with this serializer,
             * as it will be used to deserialize the data retrieved via [itemDataJs].
             */
            itemDataSerializer: KSerializer<D>,
            /**
             * Factory function to create an item instance from its container element. This instance is used
             * for active interactions, when actions need to be performed on the item.
             */
            itemFactory: (container: Locator) -> I,
        ): SaPageableItems<I, D> {
            val container = page.locator(".sa-pageable-items")
            return SaPageableItems(
                container = container,
                paginator = this.twoSyncedPaginators(container),
                itemFactory = itemFactory,
                itemDataJs = itemDataJs,
                itemDataSerializer = itemDataSerializer,
            )
        }
    }
}
