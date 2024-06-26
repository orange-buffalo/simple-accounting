package io.orangebuffalo.simpleaccounting.domain.documents

import io.orangebuffalo.simpleaccounting.infra.SimpleAccountingIntegrationTest
import io.orangebuffalo.simpleaccounting.web.AbstractFilteringApiTest
import io.orangebuffalo.simpleaccounting.web.generateFilteringApiTests

@SimpleAccountingIntegrationTest
class DocumentsFilteringApiIT : AbstractFilteringApiTest() {

    companion object {
        @Suppress("unused")
        @JvmStatic
        fun createTestCases() =
            generateFilteringApiTests<Document> {

                baseUrl = "documents"

                entityMatcher {
                    responseFields("name")
                    entityFields({ document -> document.name })
                }

                val idApiField = "id"
                val invoicePlaceholder = "{INVOICE}"
                val receiptPlaceholder = "{RECEIPT}"
                val idEqInvoiceFilter = "$idApiField[eq]=$invoicePlaceholder"
                val idInInvoiceReceiptFilter = "$idApiField[in]=$invoicePlaceholder&$idApiField[in]=$receiptPlaceholder"

                filtering {
                    entity {
                        createEntity {
                            entitiesFactory.document(
                                workspace = targetWorkspace,
                                name = "Invoice",
                            )
                        }
                        dynamicFilterReplacement(invoicePlaceholder) { document -> document.id }
                    }

                    entity {
                        createEntity {
                            entitiesFactory.document(
                                workspace = targetWorkspace,
                                name = "Receipt",
                            )
                        }
                        dynamicFilterReplacement(receiptPlaceholder) { document -> document.id }
                        skippedOn(idEqInvoiceFilter)
                    }

                    entity {
                        createEntity {
                            entitiesFactory.document(
                                workspace = targetWorkspace,
                                name = "Contract",
                            )
                        }
                        skippedOn(idEqInvoiceFilter)
                        skippedOn(idInInvoiceReceiptFilter)
                    }
                }
            }
    }
}
