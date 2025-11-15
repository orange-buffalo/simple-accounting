package io.orangebuffalo.simpleaccounting.tests.infra.ui.components

import io.orangebuffalo.simpleaccounting.tests.infra.utils.visualToData

class SaDocumentsList {

    companion object {

        private const val VISUAL_SEMANTIC = "documents"

        /**
         * Tests can use this method to produce documents list data value from document names.
         * This is used for JS-based data extractors, like [SaPageableItems].
         */
        fun documentsValue(vararg documentNames: String): String = 
            visualToData(VISUAL_SEMANTIC, documentNames.joinToString(", "))

        /**
         * JavaScript function that extracts the documents list content from any element inside the documents list component.
         * Used in JS-based data extractors, like [SaPageableItems], indirectly via
         * [io.orangebuffalo.simpleaccounting.tests.infra.utils.injectJsUtils] `getDynamicContent`.
         * Not intended for direct use in tests.
         */
        fun jsDataExtractor() = /* language=JavaScript */ """
            (anyElement) => {
                const documentsListElement = utils.findClosestByClass(anyElement, 'sa-documents-list');
                if (!documentsListElement) {
                    return null;
                }
                
                // Check if storage is loading
                const loadingPlaceholder = documentsListElement.querySelector('.sa-documents-list__loading-placeholder');
                if (loadingPlaceholder) {
                    return '<loading>';
                }
                
                // Check if storage is not active (shows an error alert)
                const failedStorageMessage = documentsListElement.querySelector('.el-alert--error');
                if (failedStorageMessage) {
                    return '<storage not active>';
                }
                
                // Find all document elements (using the root class from SaDocument component)
                const documentElements = Array.from(documentsListElement.querySelectorAll('.sa-document'));
                if (documentElements.length === 0) {
                    return null;
                }
                
                // Check if any documents are still loading (have loader elements)
                const hasLoadingDocuments = documentElements.some(doc => {
                    return doc.querySelector('.sa-document__loader__file-icon') !== null;
                });
                if (hasLoadingDocuments) {
                    return '<document loading>';
                }
                
                // Extract document names from loaded documents
                const documentNames = documentElements
                    .map(doc => {
                        const nameElement = doc.querySelector('.sa-document__file-description__header__file-name');
                        const textContent = utils.transformTextContent(nameElement ? nameElement.textContent : null);
                        return textContent || '';
                    })
                    .filter(name => name !== '');
                
                return documentNames.length > 0 
                    ? utils.visualToData('$VISUAL_SEMANTIC', documentNames.join(', '))
                    : null;
            }
        """
    }
}
