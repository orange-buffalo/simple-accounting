package io.orangebuffalo.simpleaccounting.business.integrations

import io.orangebuffalo.simpleaccounting.business.incomes.Income
import io.orangebuffalo.simpleaccounting.business.invoices.Invoice
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.Repository

interface AccountStatementProcessingRepository : Repository<AccountStatementRecord, String> {
    @Query("""
        select r.* from account_statement_record r
        where r.workspace_id = :workspaceId and r.type = 'CREDIT' and r.linked_invoice_id is null
    """)
    fun findCreditsWithoutInvoice(workspaceId: String): List<AccountStatementRecord>

    @Query("""
        select i.* from invoice i join customer c on c.id = i.customer_id
        where c.workspace_id = :workspaceId
          and not exists (select 1 from account_statement_record r where r.linked_invoice_id = i.id)
    """)
    fun findUnlinkedInvoices(workspaceId: String): List<Invoice>

    @Query("""
        select r.* from account_statement_record r
        where r.workspace_id = :workspaceId and r.integration_id = :integrationId
          and r.type = 'CREDIT' and r.linked_income_id is null and r.linked_invoice_id is not null
          and not exists (select 1 from income i where i.linked_invoice_id = r.linked_invoice_id and i.workspace_id = r.workspace_id)
          and not exists (select 1 from invoice i join customer c on c.id = i.customer_id
                          where i.id = r.linked_invoice_id and c.workspace_id = r.workspace_id
                           and (i.date_paid is not null or i.status in ('PAID', 'CANCELLED')))
    """)
    fun findCreditsForIncomeCreation(workspaceId: String, integrationId: String): List<AccountStatementRecord>

    @Query("""
        select i.* from invoice i join customer c on c.id = i.customer_id
        where i.id in (:ids) and c.workspace_id = :workspaceId
    """)
    fun findInvoices(ids: List<String>, workspaceId: String): List<Invoice>

    @Query("""
        select i.* from income i
        where i.workspace_id = :workspaceId and i.converted_original_amount_in_default_currency is null
          and exists (select 1 from account_statement_record r
                      where r.linked_invoice_id = i.linked_invoice_id and r.integration_id = :integrationId)
    """)
    fun findIncomesForConversion(workspaceId: String, integrationId: String): List<Income>

    @Query("""
        select r.* from account_statement_record r
        where r.workspace_id = :workspaceId and r.integration_id = :integrationId
          and r.type = 'CONVERSION' and r.linked_income_id is null
          and r.source_amount_in_cents > 0 and r.target_amount_in_cents >= 0
          and r.source_currency is not null and r.target_currency = :defaultCurrency
    """)
    fun findConversions(workspaceId: String, integrationId: String, defaultCurrency: String): List<AccountStatementRecord>
}
