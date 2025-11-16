package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import io.orangebuffalo.simpleaccounting.tests.infra.utils.dataValues

class SaActionLink {

    companion object {
        /**
         * Tests can use this method to produce action link data value.
         * This is used for JS-based data extractors, like [SaPageableItems].
         */
        fun actionLinkValue(icon: SaIconType, label: String): String =
            dataValues(SaIcon.iconValue(icon), label)

        /**
         * Tests can use this method to produce edit action link data value.
         */
        fun editActionLinkValue(label: String = "Edit"): String =
            actionLinkValue(SaIconType.PENCIL_SOLID, label)

        /**
         * Tests can use this method to produce copy action link data value.
         */
        fun copyActionLinkValue(label: String = "Copy"): String =
            actionLinkValue(SaIconType.COPY, label)

        /**
         * Tests can use this method to produce mark as sent action link data value.
         */
        fun markAsSentActionLinkValue(label: String = "Mark as Sent"): String =
            actionLinkValue(SaIconType.SEND_SOLID, label)

        /**
         * Tests can use this method to produce mark as paid action link data value.
         */
        fun markAsPaidActionLinkValue(label: String = "Mark as Paid"): String =
            actionLinkValue(SaIconType.INCOME_SOLID, label)
    }
}
