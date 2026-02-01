<template>
  <SaOverviewItem :title="invoice.title">
    <template #primary-attributes>
      <SaOverviewItemPrimaryAttribute
        :tooltip="$t.invoicesOverviewPanel.customer.tooltip()"
        icon="customer"
      >
        <SaCustomerOutput :customer-id="invoice.customer" />
      </SaOverviewItemPrimaryAttribute>

      <SaOverviewItemPrimaryAttribute
        v-if="invoice.datePaid"
        :tooltip="$t.invoicesOverviewPanel.datePaid.tooltip()"
        icon="calendar"
      >
        {{ $t.common.date.medium(invoice.datePaid) }}
      </SaOverviewItemPrimaryAttribute>
    </template>

    <template #attributes-preview>
      <SaOverviewItemAttributePreviewIcon
        v-if="invoice.notes"
        icon="notes"
        :tooltip="$t.invoicesOverviewPanel.notes.tooltip()"
      />

      <SaOverviewItemAttributePreviewIcon
        v-if="invoice.attachments.length"
        :tooltip="$t.invoicesOverviewPanel.attachments.tooltip()"
        icon="attachment"
      />

      <SaOverviewItemAttributePreviewIcon
        v-if="isGeneralTaxApplicable"
        :tooltip="$t.invoicesOverviewPanel.generalTax.tooltip()"
        icon="tax"
      />

      <SaOverviewItemAttributePreviewIcon
        v-if="isForeignCurrency"
        :tooltip="$t.invoicesOverviewPanel.foreignCurrency.tooltip()"
        icon="multi-currency"
      />
    </template>

    <template #middle-column>
      <SaStatusLabel
        :status="statusInfo.statusValue"
        :custom-icon="statusInfo.statusIcon"
      >
        {{ statusInfo.statusText }}
      </SaStatusLabel>
    </template>

    <template #last-column>
      <SaOverviewItemAmountPanel
        :currency="invoice.currency"
        :amount="invoice.amount"
      />
    </template>

    <template #details>
      <SaOverviewItemDetailsSectionActions>
        <SaActionLink
          v-if="currentWorkspace.editable"
          icon="pencil-solid"
          @click="navigateToInvoiceEdit"
        >
          {{ $t.invoicesOverviewPanel.edit() }}
        </SaActionLink>

        <SaActionLink
          v-if="currentWorkspace.editable && statusInfo.isDraft"
          icon="send-solid"
          @click="markSent"
        >
          {{ $t.invoicesOverviewPanel.markAsSent() }}
        </SaActionLink>

        <SaActionLink
          v-if="currentWorkspace.editable && (statusInfo.isSent || statusInfo.isOverdue)"
          icon="income-solid"
          @click="markPaid"
        >
          {{ $t.invoicesOverviewPanel.markAsPaid() }}
        </SaActionLink>
      </SaOverviewItemDetailsSectionActions>

      <SaOverviewItemDetailsSection
        :title="$t.invoicesOverviewPanel.generalInformation.header()"
      >
        <div class="row">
          <SaOverviewItemDetailsSectionAttribute
            :label="$t.invoicesOverviewPanel.status.label()"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            <SaStatusLabel
              :status="statusInfo.statusValue"
              :custom-icon="statusInfo.statusIcon"
              :simplified="true"
            >
              {{ statusInfo.statusText }}
            </SaStatusLabel>
          </SaOverviewItemDetailsSectionAttribute>

          <SaOverviewItemDetailsSectionAttribute
            :label="$t.invoicesOverviewPanel.customer.label()"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            <SaCustomerOutput :customer-id="invoice.customer" />
          </SaOverviewItemDetailsSectionAttribute>

          <SaOverviewItemDetailsSectionAttribute
            v-if="isForeignCurrency"
            :label="$t.invoicesOverviewPanel.currency.label()"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            {{ invoice.currency }}
          </SaOverviewItemDetailsSectionAttribute>

          <SaOverviewItemDetailsSectionAttribute
            :label="$t.invoicesOverviewPanel.amount.label()"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            <SaMoneyOutput
              :currency="invoice.currency"
              :amount-in-cents="invoice.amount"
            />
          </SaOverviewItemDetailsSectionAttribute>
        </div>

        <div class="row">
          <SaOverviewItemDetailsSectionAttribute
            :label="$t.invoicesOverviewPanel.dateIssued.label()"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            {{ $t.common.date.medium(invoice.dateIssued) }}
          </SaOverviewItemDetailsSectionAttribute>

          <SaOverviewItemDetailsSectionAttribute
            :label="$t.invoicesOverviewPanel.dueDate.label()"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            {{ $t.common.date.medium(invoice.dueDate) }}
          </SaOverviewItemDetailsSectionAttribute>
        </div>

        <div
          v-if="invoice.dateSent || invoice.datePaid"
          class="row"
        >
          <SaOverviewItemDetailsSectionAttribute
            v-if="invoice.dateSent"
            :label="$t.invoicesOverviewPanel.dateSent.label()"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            {{ $t.common.date.medium(invoice.dateSent) }}
          </SaOverviewItemDetailsSectionAttribute>

          <SaOverviewItemDetailsSectionAttribute
            v-if="invoice.datePaid"
            :label="$t.invoicesOverviewPanel.datePaid.label()"
            class="col col-xs-12 col-md-6 col-lg-4"
          >
            {{ $t.common.date.medium(invoice.datePaid) }}
          </SaOverviewItemDetailsSectionAttribute>
        </div>

        <template v-if="isGeneralTaxApplicable">
          <div class="row">
            <SaOverviewItemDetailsSectionAttribute
              :label="$t.invoicesOverviewPanel.generalTax.label()"
              class="col col-xs-12 col-md-6 col-lg-4"
            >
              <SaGeneralTaxOutput :general-tax-id="invoice.generalTax" />
            </SaOverviewItemDetailsSectionAttribute>

            <SaOverviewItemDetailsSectionAttribute
              :label="$t.invoicesOverviewPanel.generalTaxRate.label()"
              class="col col-xs-12 col-md-6 col-lg-4"
            >
              {{ $t.invoicesOverviewPanel.generalTaxRate.value(generalTaxRate) }}
            </SaOverviewItemDetailsSectionAttribute>
          </div>
        </template>
      </SaOverviewItemDetailsSection>

      <SaOverviewItemDetailsSection
        v-if="invoice.attachments.length"
        :title="$t.invoicesOverviewPanel.attachments.header()"
      >
        <div class="row">
          <div class="col col-xs-12">
            <SaDocumentsList :documents-ids="invoice.attachments" />
          </div>
        </div>
      </SaOverviewItemDetailsSection>

      <SaOverviewItemDetailsSection
        v-if="invoice.notes"
        :title="$t.invoicesOverviewPanel.notes.header()"
      >
        <div class="row">
          <div class="col col-xs-12">
            <SaMarkdownOutput :source="invoice.notes" />
          </div>
        </div>
      </SaOverviewItemDetailsSection>
    </template>
  </SaOverviewItem>
</template>

<script lang="ts" setup>
  import { watch, computed, ref } from 'vue';
  import SaMoneyOutput from '@/components/SaMoneyOutput.vue';
  import SaOverviewItem from '@/components/overview-item/SaOverviewItem.vue';
  import SaOverviewItemAmountPanel from '@/components/overview-item/SaOverviewItemAmountPanel.vue';
  import SaOverviewItemAttributePreviewIcon from '@/components/overview-item/SaOverviewItemAttributePreviewIcon.vue';
  import SaOverviewItemDetailsSection from '@/components/overview-item/SaOverviewItemDetailsSection.vue';
  import SaOverviewItemDetailsSectionActions from '@/components/overview-item/SaOverviewItemDetailsSectionActions.vue';
  import SaOverviewItemDetailsSectionAttribute
    from '@/components/overview-item/SaOverviewItemDetailsSectionAttribute.vue';
  import SaOverviewItemPrimaryAttribute from '@/components/overview-item/SaOverviewItemPrimaryAttribute.vue';
  import SaActionLink from '@/components/SaActionLink.vue';
  import SaDocumentsList from '@/components/documents/SaDocumentsList.vue';
  import SaMarkdownOutput from '@/components/SaMarkdownOutput.vue';
  import SaStatusLabel, { type StatusLabelStatus } from '@/components/SaStatusLabel.vue';
  import SaCustomerOutput from '@/components/customer/SaCustomerOutput.vue';
  import SaGeneralTaxOutput from '@/components/general-tax/SaGeneralTaxOutput.vue';
  import useNavigation from '@/services/use-navigation';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import type { InvoiceDto } from '@/services/api';
  import { generalTaxesApi, invoicesApi } from '@/services/api';
  import { $t } from '@/services/i18n';

  const props = defineProps<{
    invoice: InvoiceDto
  }>();

  const emit = defineEmits<{(e: 'invoice-update'): void }>();

  const {
    currentWorkspace,
    currentWorkspaceId,
    defaultCurrency,
  } = useCurrentWorkspace();

  const markSent = async () => {
    const invoiceRequest = {
      ...props.invoice,
      dateSent: new Date().toISOString().substring(0, 10),
    };
    await invoicesApi.updateInvoice({
      invoiceId: props.invoice.id,
      workspaceId: currentWorkspaceId,
      editInvoiceDto: invoiceRequest,
    });
    emit('invoice-update');
  };

  const { navigateToView } = useNavigation();

  const navigateToInvoiceEdit = () => {
    navigateToView({
      name: 'edit-invoice',
      params: { id: props.invoice.id },
    });
  };

  const markPaid = () => {
    navigateToView({
      name: 'create-new-income',
      params: { sourceInvoiceId: props.invoice.id },
    });
  };

  const statusInfo = computed<{
    isDraft?: boolean,
    isPaid?: boolean,
    isCancelled?: boolean,
    isSent?: boolean,
    isOverdue?: boolean,
    statusIcon?: string,
    statusValue: StatusLabelStatus,
    statusText?: string
  }>(() => {
    switch (props.invoice.status) {
    case 'DRAFT':
      return {
        isDraft: true,
        statusIcon: 'draft',
        statusValue: 'regular',
        statusText: $t.value.invoicesOverviewPanel.status.draft(),
      };
    case 'PAID':
      return {
        isPaid: true,
        statusValue: 'success',
        statusText: $t.value.invoicesOverviewPanel.status.finalized(),
      };
    case 'CANCELLED':
      return {
        isCancelled: true,
        statusIcon: 'cancel',
        statusValue: 'regular',
        statusText: $t.value.invoicesOverviewPanel.status.cancelled(),
      };
    case 'SENT':
      return {
        isSent: true,
        statusValue: 'pending',
        statusText: $t.value.invoicesOverviewPanel.status.sent(),
      };
    case 'OVERDUE':
      return {
        isOverdue: true,
        statusValue: 'failure',
        statusText: $t.value.invoicesOverviewPanel.status.overdue(),
      };
    default:
      throw new Error(`Unknown invoice status ${props.invoice.status}`);
    }
  });

  const isGeneralTaxApplicable = computed(() => props.invoice.generalTax != null);
  const isForeignCurrency = computed(() => props.invoice.currency !== defaultCurrency);

  const generalTaxRate = ref(0);
  watch(() => props.invoice.generalTax, async (taxId) => {
    if (taxId !== undefined) {
      const generalTax = await generalTaxesApi.getTax({
        workspaceId: currentWorkspaceId,
        taxId,
      });
      generalTaxRate.value = generalTax.rateInBps;
    }
  }, { immediate: true });
</script>
