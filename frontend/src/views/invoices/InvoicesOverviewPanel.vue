<template>
  <OverviewItem :title="invoice.title">
    <template v-slot:primary-attributes>
      <OverviewItemPrimaryAttribute
        v-if="customer.name"
        :tooltip="$t('invoicesOverviewPanel.customer.tooltip')"
        icon="customer"
      >
        {{ customer.name }}
      </OverviewItemPrimaryAttribute>

      <OverviewItemPrimaryAttribute
        v-if="invoice.datePaid"
        :tooltip="$t('invoicesOverviewPanel.datePaid.tooltip')"
        icon="calendar"
      >
        {{ $t('common.date.medium', [invoice.datePaid]) }}
      </OverviewItemPrimaryAttribute>
    </template>

    <template v-slot:attributes-preview>
      <OverviewItemAttributePreviewIcon
        v-if="invoice.notes"
        icon="notes"
        :tooltip="$t('invoicesOverviewPanel.notes.tooltip')"
      />

      <OverviewItemAttributePreviewIcon
        v-if="invoice.attachments.length"
        :tooltip="$t('invoicesOverviewPanel.attachments.tooltip')"
        icon="attachment"
      />

      <OverviewItemAttributePreviewIcon
        v-if="isGeneralTaxApplicable"
        :tooltip="$t('invoicesOverviewPanel.generalTax.tooltip')"
        icon="tax"
      />

      <OverviewItemAttributePreviewIcon
        v-if="isForeignCurrency"
        :tooltip="$t('invoicesOverviewPanel.foreignCurrency.tooltip')"
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
          {{ $t('invoicesOverviewPanel.edit') }}
        </SaActionLink>

        <SaActionLink
          v-if="currentWorkspace.editable && isDraft"
          icon="send-solid"
          @click="markSent"
        >
          {{ $t('invoicesOverviewPanel.markAsSent') }}
        </SaActionLink>

        <SaActionLink
          v-if="currentWorkspace.editable && (isSent || isOverdue)"
          icon="income-solid"
          @click="markPaid"
        >
          {{ $t('invoicesOverviewPanel.markAsPaid') }}
        </SaActionLink>
      </OverviewItemDetailsSectionActions>

      <OverviewItemDetailsSection
        :title="$t('invoicesOverviewPanel.generalInformation.header')"
      >
        <div class="row">
          <OverviewItemDetailsSectionAttribute
            :label="$t('invoicesOverviewPanel.status.label')"
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
            :label="$t('invoicesOverviewPanel.customer.label')"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            {{ customer.name }}
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute
            :label="$t('invoicesOverviewPanel.category.label')"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            {{ categoryById(invoice.category).name }}
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute
            v-if="isForeignCurrency"
            :label="$t('invoicesOverviewPanel.currency.label')"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            {{ invoice.currency }}
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute
            :label="$t('invoicesOverviewPanel.amount.label')"
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
            :label="$t('invoicesOverviewPanel.dateIssued.label')"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            {{ $t('common.date.medium', [invoice.dateIssued]) }}
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute
            :label="$t('invoicesOverviewPanel.dueDate.label')"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            {{ $t('common.date.medium', [invoice.dueDate]) }}
          </OverviewItemDetailsSectionAttribute>
        </div>

        <div
          v-if="invoice.dateSent || invoice.dateCancelled || invoice.datePaid"
          class="row"
        >
          <OverviewItemDetailsSectionAttribute
            v-if="invoice.dateSent"
            :label="$t('invoicesOverviewPanel.dateSent.label')"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            {{ $t('common.date.medium', [invoice.dateSent]) }}
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute
            v-if="invoice.dateCancelled"
            :label="$t('invoicesOverviewPanel.dateCancelled.label')"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            {{ $t('common.date.medium', [invoice.dateCancelled]) }}
          </OverviewItemDetailsSectionAttribute>

          <OverviewItemDetailsSectionAttribute
            v-if="invoice.datePaid"
            :label="$t('invoicesOverviewPanel.datePaid.label')"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            {{ $t('common.date.medium', [invoice.datePaid]) }}
          </OverviewItemDetailsSectionAttribute>
        </div>

        <template v-if="isGeneralTaxApplicable">
          <div class="row">
            <OverviewItemDetailsSectionAttribute
              :label="$t('invoicesOverviewPanel.generalTax.label')"
              class="col col-xs-12 col-md-6 col-lg-4"
            >
              {{ generalTaxTitle }}
            </OverviewItemDetailsSectionAttribute>

            <OverviewItemDetailsSectionAttribute
              :label="$t('invoicesOverviewPanel.generalTaxRate.label')"
              class="col col-xs-12 col-md-6 col-lg-4"
            >
              {{ $t('invoicesOverviewPanel.generalTaxRate.value', [generalTaxById(invoice.generalTax).rateInBps]) }}
            </OverviewItemDetailsSectionAttribute>
          </div>
        </template>
      </OverviewItemDetailsSection>

      <OverviewItemDetailsSection
        v-if="invoice.attachments.length"
        :title="$t('invoicesOverviewPanel.attachments.header')"
      >
        <div class="row">
          <div class="col col-xs-12">
            <SaDocumentsList :documents-ids="invoice.attachments" />
          </div>
        </div>
      </OverviewItemDetailsSection>

      <OverviewItemDetailsSection
        v-if="invoice.notes"
        :title="$t('invoicesOverviewPanel.notes.header')"
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
    name: 'InvoicesOverviewPanel',

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

    mixins: [withWorkspaces, withCategories, withCustomers, withGeneralTaxes],

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
          return this.$t('invoicesOverviewPanel.status.finalized');
        }
        if (this.isDraft) {
          return this.$t('invoicesOverviewPanel.status.draft');
        }
        if (this.isCancelled) {
          return this.$t('invoicesOverviewPanel.status.cancelled');
        }
        if (this.isSent) {
          return this.$t('invoicesOverviewPanel.status.sent');
        }
        if (this.isOverdue) {
          return this.$t('invoicesOverviewPanel.status.overdue');
        }
        return null;
      },

      isGeneralTaxApplicable() {
        return this.invoice.generalTax && this.generalTaxTitle;
      },

      generalTaxTitle() {
        return this.generalTaxById(this.invoice.generalTax).title;
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
