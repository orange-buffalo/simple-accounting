package io.orangebuffalo.simpleaccounting.domain.documents

import io.orangebuffalo.simpleaccounting.Prototypes
import io.orangebuffalo.simpleaccounting.junit.SimpleAccountingIntegrationTest
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
                val idInInvoiceReceiptFilter = "$idApiField[in]=$invoicePlaceholder&$idApiField[in]=$receiptPlaceholder"

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
