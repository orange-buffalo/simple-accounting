package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import io.orangebuffalo.simpleaccounting.tests.infra.utils.visualToData

class SaMarkdownOutput {

    companion object {

        private const val VISUAL_SEMANTIC = "markdown"

        /**
         * Tests can use this method to produce markdown data value from the content.
         * This is used for JS-based data extractors, like [SaPageableItems].
         */
        fun markdownValue(content: String): String = visualToData(VISUAL_SEMANTIC, content)

        /**
         * JavaScript function that extracts the markdown content from any element inside the markdown component.
         * Used in JS-based data extractors, like [SaPageableItems], indirectly via
         * [io.orangebuffalo.simpleaccounting.tests.infra.utils.injectJsUtils] `getDynamicContent`.
         * Not intended for direct use in tests.
         */
        fun jsDataExtractor() = /* language=JavaScript */ """
            (anyElement) => {
                const markdownElement = utils.findClosestByClass(anyElement, 'markdown-output');
                if (!markdownElement) {
                    return null;
                }
                // Get the text content, trimmed and normalized
                const textContent = utils.transformTextContent(markdownElement.textContent);
                return textContent ? utils.visualToData('$VISUAL_SEMANTIC', textContent) : null;
            }
        """
    }
}
