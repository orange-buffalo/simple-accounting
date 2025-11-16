package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import io.orangebuffalo.simpleaccounting.tests.infra.utils.visualToData

class SaIcon {

    companion object {

        private const val VISUAL_SEMANTIC = "icon"

        /**
         * Tests can use this method to produce icon data value from the [SaIconType].
         * This is used for JS-based data extractors, like [SaPageableItems].
         */
        fun iconValue(iconType: SaIconType): String = visualToData(VISUAL_SEMANTIC, iconType.uiValue)

        /**
         * JavaScript function that extracts the icon data value from any element inside the icon component.
         * Used in JS-based data extractors, like [SaPageableItems], indirectly via
         * [io.orangebuffalo.simpleaccounting.tests.infra.utils.injectJsUtils] `getDynamicContent`.
         * Not intended for direct use in tests.
         */
        fun jsDataExtractor() = /* language=JavaScript */ """
            (anyElement) => {
                const iconElement = utils.findClosestByClass(anyElement, 'sa-icon');
                if (!iconElement) {
                    return null;
                }
                const iconName = iconElement.getAttribute('data-icon');
                return utils.visualToData('$VISUAL_SEMANTIC', iconName);
            }
        """
    }
}

enum class SaIconType(val uiValue: String) {
    ATTACHMENT("attachment"),
    MULTI_CURRENCY("multi-currency"),
    NOTES("notes"),
    PERCENT("percent"),
    TAX("tax"),
    CALENDAR("calendar"),
    ADMIN_USER("admin-user"),
    REGULAR_USER("regular-user"),
    ACTIVE_USER("active-user"),
    INACTIVE_USER("inactive-user"),
    PENCIL_SOLID("pencil-solid"),
    COPY("copy"),
    CUSTOMER("customer"),
    SEND_SOLID("send-solid"),
    INCOME_SOLID("income-solid"),
    INVOICE("invoice"),
}
