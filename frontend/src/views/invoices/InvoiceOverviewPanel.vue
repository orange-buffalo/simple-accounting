<template>
  <OverviewItem :title="invoice.title"
                @details-shown="loadAttachments">
    <template v-slot:primary-attributes>
      <OverviewItemPrimaryAttribute v-if="customer"
                                    tooltip="Customer"
                                    icon="customer">
        {{customer.name}}
      </OverviewItemPrimaryAttribute>

      <OverviewItemPrimaryAttribute v-if="datePaid"
                                    tooltip="Date paid"
                                    icon="calendar">
        {{datePaid}}
      </OverviewItemPrimaryAttribute>
    </template>

    <template v-slot:attributes-preview>
      <OverviewItemAttributePreviewIcon v-if="invoice.notes"
                                        icon="notes"
                                        tooltip="Additional notes provided"/>

      <OverviewItemAttributePreviewIcon v-if="invoice.attachments.length"
                                        tooltip="Attachments provided"
                                        icon="attachment"/>

      <OverviewItemAttributePreviewIcon v-if="isTaxApplicable"
                                        tooltip="Tax applied"
                                        icon="tax"/>

      <OverviewItemAttributePreviewIcon v-if="isForeignCurrency"
                                        tooltip="In foreign currency"
                                        icon="multi-currency"/>
    </template>

    <template v-slot:middle-column>
      <SaStatusLabel :status="status"
                     :custom-icon="statusIcon">{{ statusText }}
      </SaStatusLabel>
    </template>

    <template v-slot:last-column>
      <OverviewItemAmountPanel :currency="invoice.currency"
                               :amount="invoice.amount"/>
    </template>

    <template v-slot:details>
      <OverviewItemDetailsSectionActions>
        <SaActionLink icon="pencil-solid"
                      v-if="currentWorkspace.editable"
                      @click="navigateToInvoiceEdit">
          Edit
        </SaActionLink>

        <SaActionLink icon="send-solid"
                      v-if="currentWorkspace.editable && isDraft"
                      @click="markSent">
          Sent today
        </SaActionLink>

        <SaActionLink icon="income-solid"
                      v-if="currentWorkspace.editable && (isSent || isOverdue)"
                      @click="markPaid">
          Paid today
        </SaActionLink>
      </OverviewItemDetailsSectionActions>

      <OverviewItemDetailsSection title="General Information">
        <div class="row">
          <OverviewItemDetailsSectionAttribute label="Status"
                                               class="col col-xs-12 col-md-6 col-lg-4">
            <SaStatusLabel :status="status"
                           :custom-icon="statusIcon"
                           :simplified="true">
              {{ statusText }}
            </SaStatusLabel>
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute label="Customer"
                                               class="col col-xs-12 col-md-6 col-lg-4">
            {{ customer.name }}
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute label="Category"
                                               class="col col-xs-12 col-md-6 col-lg-4">
            {{ categoryById(invoice.category).name }}
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute v-if="isForeignCurrency"
                                               label="Invoice Currency"
                                               class="col col-xs-12 col-md-6 col-lg-4">
            {{invoice.currency}}
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute label="Invoice Amount"
                                               class="col col-xs-12 col-md-6 col-lg-4">
            <MoneyOutput :currency="invoice.currency"
                         :amount="invoice.amount"/>
          </OverviewItemDetailsSectionAttribute>
        </div>

        <div class="row">
          <OverviewItemDetailsSectionAttribute label="Date Issued"
                                               class="col col-xs-12 col-md-6 col-lg-4">
            {{dateIssued}}
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute label="Due Date"
                                               class="col col-xs-12 col-md-6 col-lg-4">
            {{dueDate}}
          </OverviewItemDetailsSectionAttribute>
        </div>

        <div class="row"
             v-if="dateSent || dateCancelled || datePaid">
          <OverviewItemDetailsSectionAttribute v-if="dateSent"
                                               label="Date Sent"
                                               class="col col-xs-12 col-md-6 col-lg-4">
            {{dateSent}}
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute v-if="dateCancelled"
                                               label="Date Cancelled"
                                               class="col col-xs-12 col-md-6 col-lg-4">
            {{dateCancelled}}
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute v-if="datePaid"
                                               label="Date Paid"
                                               class="col col-xs-12 col-md-6 col-lg-4">
            {{datePaid}}
          </OverviewItemDetailsSectionAttribute>
        </div>

        <template v-if="isTaxApplicable">
          <div class="row">
            <OverviewItemDetailsSectionAttribute label="Applicable Tax"
                                                 class="col col-xs-12 col-md-6 col-lg-4">
              {{taxTitle}}
            </OverviewItemDetailsSectionAttribute>

            <OverviewItemDetailsSectionAttribute label="Applicable Tax Rate"
                                                 class="col col-xs-12 col-md-6 col-lg-4">
              <!-- todo #6 localize-->
              {{taxById(invoice.tax).rateInBps / 100}}%
            </OverviewItemDetailsSectionAttribute>
          </div>
        </template>
      </OverviewItemDetailsSection>

      <OverviewItemDetailsSection title="Attachments"
                                  v-if="attachments.length">
        <div class="row">
          <div class="col col-xs-12">
            <span v-for="attachment in attachments"
                  :key="attachment.id">
             <document-link :document="attachment"/><br/>
            </span>
          </div>
        </div>
      </OverviewItemDetailsSection>

      <OverviewItemDetailsSection title="Additional Notes"
                                  v-if="invoice.notes">
        <div class="row">
          <div class="col col-xs-12">
            <SaMarkdownOutput :source="invoice.notes"/>
          </div>
        </div>
      </OverviewItemDetailsSection>
    </template>
  </OverviewItem>
</template>

<script>
  import MoneyOutput from '@/components/MoneyOutput'
  import DocumentLink from '@/components/DocumentLink'
  import {withMediumDateFormatter} from '@/components/mixins/with-medium-date-formatter'
  import api from '@/services/api'
  import {withCategories} from '@/components/mixins/with-categories'
  import {withWorkspaces} from '@/components/mixins/with-workspaces'
  import {loadDocuments} from '@/services/app-services'
  import {withCustomers} from '@/components/mixins/with-customers'
  import {withTaxes} from '@/components/mixins/with-taxes'
  import OverviewItemDetailsSectionAttribute from '@/components/overview-item/OverviewItemDetailsSectionAttribute'
  import OverviewItemDetailsSection from '@/components/overview-item/OverviewItemDetailsSection'
  import OverviewItemPrimaryAttribute from '@/components/overview-item/OverviewItemPrimaryAttribute'
  import OverviewItemAttributePreviewIcon from '@/components/overview-item/OverviewItemAttributePreviewIcon'
  import SaStatusLabel from '@/components/SaStatusLabel'
  import OverviewItemAmountPanel from '@/components/overview-item/OverviewItemAmountPanel'
  import OverviewItemDetailsSectionActions from '@/components/overview-item/OverviewItemDetailsSectionActions'
  import SaActionLink from '@/components/SaActionLink'
  import SaIcon from '@/components/SaIcon'
  import OverviewItem from '@/components/overview-item/OverviewItem'
  import SaMarkdownOutput from '@/components/SaMarkdownOutput'

  export default {
    name: 'InvoiceOverviewPanel',

    mixins: [withMediumDateFormatter, withWorkspaces, withCategories, withCustomers, withTaxes],

    components: {
      MoneyOutput,
      DocumentLink,
      OverviewItem,
      SaIcon,
      OverviewItemAttributePreviewIcon,
      OverviewItemPrimaryAttribute,
      OverviewItemDetailsSection,
      OverviewItemDetailsSectionAttribute,
      SaActionLink,
      OverviewItemDetailsSectionActions,
      OverviewItemAmountPanel,
      SaStatusLabel,
      SaMarkdownOutput
    },

    props: {
      invoice: {
        type: Object,
        required: true
      }
    },

    data: function () {
      return {
        notesVisible: false,
        attachmentsVisible: false,
        attachments: []
      }
    },

    computed: {
      isDraft: function () {
        return this.invoice.status === 'DRAFT'
      },

      isPaid: function () {
        return this.invoice.status === 'PAID'
      },

      isCancelled: function () {
        return this.invoice.status === 'CANCELLED'
      },

      isSent: function () {
        return this.invoice.status === 'SENT'
      },

      isOverdue: function () {
        return this.invoice.status === 'OVERDUE'
      },

      isForeignCurrency: function () {
        return this.invoice.currency !== this.defaultCurrency
      },

      status: function () {
        if (this.isPaid) {
          return 'success'
        } else if (this.isDraft) {
          return `regular`
        } else if (this.isCancelled) {
          return `regular`
        } else if (this.isSent) {
          return `pending`
        } else if (this.isOverdue) {
          return `failure`
        }
      },

      statusIcon: function () {
        if (this.isDraft) {
          return 'draft'
        } else if (this.isCancelled) {
          return 'cancel'
        }
        return null
      },

      statusText: function () {
        if (this.isPaid) {
          return 'Finalized'
        } else if (this.isDraft) {
          return `Draft`
        } else if (this.isCancelled) {
          return `Cancelled`
        } else if (this.isSent) {
          return `Sent`
        } else if (this.isOverdue) {
          return `Overdue`
        }
      },

      isTaxApplicable: function () {
        return this.invoice.tax && this.taxTitle
      },

      taxTitle: function () {
        return this.taxById(this.invoice.tax).title
      },

      dateIssued: function () {
        return this.mediumDateFormatter(new Date(this.invoice.dateIssued))
      },

      dueDate: function () {
        return this.mediumDateFormatter(new Date(this.invoice.dueDate))
      },

      dateSent: function () {
        return this.invoice.dateSent
            ? this.mediumDateFormatter(new Date(this.invoice.dateSent))
            : null
      },

      datePaid: function () {
        return this.invoice.datePaid
            ? this.mediumDateFormatter(new Date(this.invoice.datePaid))
            : null
      },

      dateCancelled: function () {
        return this.invoice.dateCancelled
            ? this.mediumDateFormatter(new Date(this.invoice.dateCancelled))
            : null
      },

      customer: function () {
        return this.customerById(this.invoice.customer)
      }
    },

    methods: {
      loadAttachments: async function () {
        if (this.invoice.attachments.length && !this.attachments.length) {
          this.attachments = await loadDocuments(
              this.attachments,
              this.invoice.attachments,
              this.currentWorkspace.id)
        }
      },

      navigateToInvoiceEdit: function () {
        this.$router.push({name: 'edit-invoice', params: {id: this.invoice.id}})
      },

      markSent: async function () {
        this.invoice.dateSent = api.dateToString(new Date())
        await api.put(`/workspaces/${this.currentWorkspace.id}/invoices/${this.invoice.id}`, this.invoice)
        this.$emit('invoice-update')
      },

      markPaid: async function () {
        this.invoice.datePaid = api.dateToString(new Date())

        let invoiceResponse = await api
            .put(`/workspaces/${this.currentWorkspace.id}/invoices/${this.invoice.id}`, this.invoice)

        await this.$router.push({
          name: 'edit-income',
          params: {id: invoiceResponse.data.income}
        })
      }
    }
  }
</script>
