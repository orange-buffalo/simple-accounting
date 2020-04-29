package io.orangebuffalo.simpleaccounting.web.api

import io.orangebuffalo.simpleaccounting.Prototypes
import io.orangebuffalo.simpleaccounting.services.persistence.entities.Document
import io.orangebuffalo.simpleaccounting.web.AbstractFilteringApiTest
import io.orangebuffalo.simpleaccounting.web.generateFilteringApiTests

class DocumentsFilteringApiIT : AbstractFilteringApiTest() {

    companion object {
        @Suppress("unused")
        @JvmStatic
        fun createTestCases() =
            generateFilteringApiTests<Document> {

                baseUrl = "documents"

                entityMatcher {
                    responseFields("name")
                    entityFields({ invoice -> invoice.name })
                }

                defaultEntityProvider {
                    Prototypes.document(
                        workspace = workspace
                    )
                }

                val idApiField = "id"
                val invoicePlaceholder = "{INVOICE}"
                val receiptPlaceholder = "{RECEIPT}"
                val idEqInvoiceFilter = "$idApiField[eq]=$invoicePlaceholder"
                val idInInvoiceReceiptFilter = "$idApiField[in]=$invoicePlaceholder,$receiptPlaceholder"

                filtering {
                    entity {
                        configure { document -> document.name = "Invoice" }
                        dynamicFilterReplacement(invoicePlaceholder) { document -> document.id }
                    }

                    entity {
                        configure { document -> document.name = "Receipt" }
                        dynamicFilterReplacement(receiptPlaceholder) { document -> document.id }
                        skippedOn(idEqInvoiceFilter)
                    }

                    entity {
                        configure { document -> document.name = "Contract" }
                        skippedOn(idEqInvoiceFilter)
                        skippedOn(idInInvoiceReceiptFilter)
                    }
                }
            }
    }
}
