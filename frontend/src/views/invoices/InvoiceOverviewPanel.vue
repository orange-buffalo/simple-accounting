<template>
  <OverviewItem :title="invoice.title">
    <template v-slot:primary-attributes>
      <OverviewItemPrimaryAttribute
        v-if="customer"
        tooltip="Customer"
        icon="customer"
      >
        {{ customer.name }}
      </OverviewItemPrimaryAttribute>

      <OverviewItemPrimaryAttribute
        v-if="datePaid"
        tooltip="Date paid"
        icon="calendar"
      >
        {{ datePaid }}
      </OverviewItemPrimaryAttribute>
    </template>

    <template v-slot:attributes-preview>
      <OverviewItemAttributePreviewIcon
        v-if="invoice.notes"
        icon="notes"
        tooltip="Additional notes provided"
      />

      <OverviewItemAttributePreviewIcon
        v-if="invoice.attachments.length"
        tooltip="Attachments provided"
        icon="attachment"
      />

      <OverviewItemAttributePreviewIcon
        v-if="isGeneralTaxApplicable"
        tooltip="General Tax applied"
        icon="tax"
      />

      <OverviewItemAttributePreviewIcon
        v-if="isForeignCurrency"
        tooltip="In foreign currency"
        icon="multi-currency"
      />
    </template>

    <template v-slot:middle-column>
      <SaStatusLabel
        :status="status"
        :custom-icon="statusIcon"
      >
        {{ statusText }}
      </SaStatusLabel>
    </template>

    <template v-slot:last-column>
      <OverviewItemAmountPanel
        :currency="invoice.currency"
        :amount="invoice.amount"
      />
    </template>

    <template v-slot:details>
      <OverviewItemDetailsSectionActions>
        <SaActionLink
          v-if="currentWorkspace.editable"
          icon="pencil-solid"
          @click="navigateToInvoiceEdit"
        >
          Edit
        </SaActionLink>

        <SaActionLink
          v-if="currentWorkspace.editable && isDraft"
          icon="send-solid"
          @click="markSent"
        >
          Sent today
        </SaActionLink>

        <SaActionLink
          v-if="currentWorkspace.editable && (isSent || isOverdue)"
          icon="income-solid"
          @click="markPaid"
        >
          Paid today
        </SaActionLink>
      </OverviewItemDetailsSectionActions>

      <OverviewItemDetailsSection title="General Information">
        <div class="row">
          <OverviewItemDetailsSectionAttribute
            label="Status"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            <SaStatusLabel
              :status="status"
              :custom-icon="statusIcon"
              :simplified="true"
            >
              {{ statusText }}
            </SaStatusLabel>
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute
            label="Customer"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            {{ customer.name }}
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute
            label="Category"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            {{ categoryById(invoice.category).name }}
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute
            v-if="isForeignCurrency"
            label="Invoice Currency"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            {{ invoice.currency }}
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute
            label="Invoice Amount"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            <MoneyOutput
              :currency="invoice.currency"
              :amount="invoice.amount"
            />
          </OverviewItemDetailsSectionAttribute>
        </div>

        <div class="row">
          <OverviewItemDetailsSectionAttribute
            label="Date Issued"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            {{ dateIssued }}
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute
            label="Due Date"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            {{ dueDate }}
          </OverviewItemDetailsSectionAttribute>
        </div>

        <div
          v-if="dateSent || dateCancelled || datePaid"
          class="row"
        >
          <OverviewItemDetailsSectionAttribute
            v-if="dateSent"
            label="Date Sent"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            {{ dateSent }}
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute
            v-if="dateCancelled"
            label="Date Cancelled"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            {{ dateCancelled }}
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute
            v-if="datePaid"
            label="Date Paid"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            {{ datePaid }}
          </OverviewItemDetailsSectionAttribute>
        </div>

        <template v-if="isGeneralTaxApplicable">
          <div class="row">
            <OverviewItemDetailsSectionAttribute
              label="Applicable General Tax"
              class="col col-xs-12 col-md-6 col-lg-4"
            >
              {{ generalTaxTitle }}
            </OverviewItemDetailsSectionAttribute>

            <OverviewItemDetailsSectionAttribute
              label="Applicable General Tax Rate"
              class="col col-xs-12 col-md-6 col-lg-4"
            >
              <!-- todo #6 localize-->
              {{ generalTaxById(invoice.generalTax).rateInBps / 100 }}%
            </OverviewItemDetailsSectionAttribute>
          </div>
        </template>
      </OverviewItemDetailsSection>

      <OverviewItemDetailsSection
        v-if="invoice.attachments.length"
        title="Attachments"
      >
        <div class="row">
          <div class="col col-xs-12">
            <SaDocumentsList :documents-ids="invoice.attachments" />
          </div>
        </div>
      </OverviewItemDetailsSection>

      <OverviewItemDetailsSection
        v-if="invoice.notes"
        title="Additional Notes"
      >
        <div class="row">
          <div class="col col-xs-12">
            <SaMarkdownOutput :source="invoice.notes" />
          </div>
        </div>
      </OverviewItemDetailsSection>
    </template>
  </OverviewItem>
</template>

<script>
  import MoneyOutput from '@/components/MoneyOutput';
  import withCategories from '@/components/mixins/with-categories';
  import withCustomers from '@/components/mixins/with-customers';
  import withMediumDateFormatter from '@/components/mixins/with-medium-date-formatter';
  import withGeneralTaxes from '@/components/mixins/with-general-taxes';
  import withWorkspaces from '@/components/mixins/with-workspaces';
  import { api } from '@/services/api';
  import OverviewItem from '@/components/overview-item/OverviewItem';
  import OverviewItemAmountPanel from '@/components/overview-item/OverviewItemAmountPanel';
  import OverviewItemAttributePreviewIcon from '@/components/overview-item/OverviewItemAttributePreviewIcon';
  import OverviewItemDetailsSection from '@/components/overview-item/OverviewItemDetailsSection';
  import OverviewItemDetailsSectionActions from '@/components/overview-item/OverviewItemDetailsSectionActions';
  import OverviewItemDetailsSectionAttribute from '@/components/overview-item/OverviewItemDetailsSectionAttribute';
  import OverviewItemPrimaryAttribute from '@/components/overview-item/OverviewItemPrimaryAttribute';
  import SaActionLink from '@/components/SaActionLink';
  import SaDocumentsList from '@/components/documents/SaDocumentsList';
  import SaMarkdownOutput from '@/components/SaMarkdownOutput';
  import SaStatusLabel from '@/components/SaStatusLabel';

  export default {
    name: 'InvoiceOverviewPanel',

    components: {
      SaDocumentsList,
      MoneyOutput,
      OverviewItem,
      OverviewItemAttributePreviewIcon,
      OverviewItemPrimaryAttribute,
      OverviewItemDetailsSection,
      OverviewItemDetailsSectionAttribute,
      SaActionLink,
      OverviewItemDetailsSectionActions,
      OverviewItemAmountPanel,
      SaStatusLabel,
      SaMarkdownOutput,
    },

    mixins: [withMediumDateFormatter, withWorkspaces, withCategories, withCustomers, withGeneralTaxes],

    props: {
      invoice: {
        type: Object,
        required: true,
      },
    },

    computed: {
      isDraft() {
        return this.invoice.status === 'DRAFT';
      },

      isPaid() {
        return this.invoice.status === 'PAID';
      },

      isCancelled() {
        return this.invoice.status === 'CANCELLED';
      },

      isSent() {
        return this.invoice.status === 'SENT';
      },

      isOverdue() {
        return this.invoice.status === 'OVERDUE';
      },

      isForeignCurrency() {
        return this.invoice.currency !== this.defaultCurrency;
      },

      status() {
        if (this.isPaid) {
          return 'success';
        }
        if (this.isDraft) {
          return 'regular';
        }
        if (this.isCancelled) {
          return 'regular';
        }
        if (this.isSent) {
          return 'pending';
        }
        if (this.isOverdue) {
          return 'failure';
        }
        return null;
      },

      statusIcon() {
        if (this.isDraft) {
          return 'draft';
        }
        if (this.isCancelled) {
          return 'cancel';
        }
        return null;
      },

      statusText() {
        if (this.isPaid) {
          return 'Finalized';
        }
        if (this.isDraft) {
          return 'Draft';
        }
        if (this.isCancelled) {
          return 'Cancelled';
        }
        if (this.isSent) {
          return 'Sent';
        }
        if (this.isOverdue) {
          return 'Overdue';
        }
        return null;
      },

      isGeneralTaxApplicable() {
        return this.invoice.generalTax && this.generalTaxTitle;
      },

      generalTaxTitle() {
        return this.generalTaxById(this.invoice.generalTax).title;
      },

      dateIssued() {
        return this.mediumDateFormatter(new Date(this.invoice.dateIssued));
      },

      dueDate() {
        return this.mediumDateFormatter(new Date(this.invoice.dueDate));
      },

      dateSent() {
        return this.invoice.dateSent
          ? this.mediumDateFormatter(new Date(this.invoice.dateSent))
          : null;
      },

      datePaid() {
        return this.invoice.datePaid
          ? this.mediumDateFormatter(new Date(this.invoice.datePaid))
          : null;
      },

      dateCancelled() {
        return this.invoice.dateCancelled
          ? this.mediumDateFormatter(new Date(this.invoice.dateCancelled))
          : null;
      },

      customer() {
        return this.customerById(this.invoice.customer);
      },
    },

    methods: {
      navigateToInvoiceEdit() {
        this.$router.push({
          name: 'edit-invoice',
          params: { id: this.invoice.id },
        });
      },

      async markSent() {
        this.invoice.dateSent = api.dateToString(new Date());
        await api.put(`/workspaces/${this.currentWorkspace.id}/invoices/${this.invoice.id}`, this.invoice);
        this.$emit('invoice-update');
      },

      async markPaid() {
        this.invoice.datePaid = api.dateToString(new Date());

        const invoiceResponse = await api
          .put(`/workspaces/${this.currentWorkspace.id}/invoices/${this.invoice.id}`, this.invoice);

        await this.$router.push({
          name: 'edit-income',
          params: { id: invoiceResponse.data.income },
        });
      },
    },
  };
</script>
