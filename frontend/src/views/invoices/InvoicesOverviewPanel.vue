<template>
  <OverviewItem :title="invoice.title">
    <template v-slot:primary-attributes>
      <OverviewItemPrimaryAttribute
        :tooltip="$t('invoicesOverviewPanel.customer.tooltip')"
        icon="customer"
      >
        <SaCustomerOutput :customer-id="invoice.customer" />
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
        :status="statusValue"
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
              :status="statusValue"
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
            <SaCustomerOutput :customer-id="invoice.customer" />
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
              <SaGeneralTaxOutput :general-tax-id="invoice.generalTax" />
            </OverviewItemDetailsSectionAttribute>

            <OverviewItemDetailsSectionAttribute
              :label="$t('invoicesOverviewPanel.generalTaxRate.label')"
              class="col col-xs-12 col-md-6 col-lg-4"
            >
              {{ $t('invoicesOverviewPanel.generalTaxRate.value', [generalTaxRate]) }}
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
  import {
    watch, computed, toRefs, reactive,
  } from '@vue/composition-api';
  import MoneyOutput from '@/components/MoneyOutput';
  import { api } from '@/services/api';
  import i18n from '@/services/i18n';
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
  import SaCustomerOutput from '@/components/customer/SaCustomerOutput';
  import SaGeneralTaxOutput from '@/components/general-tax/SaGeneralTaxOutput';
  import useGeneralTaxes from '@/components/general-tax/useGeneralTaxes';
  import useCurrentWorkspace from '@/components/workspace/useCurrentWorkspace';
  import useNavigation from '@/components/navigation/useNavigation';

  function useInvoiceApi({ invoice, currentWorkspace, emit }) {
    async function markSent() {
      const invoiceRequest = {
        ...invoice.value,
        dateSent: api.dateToString(new Date()),
      };
      await api.put(`/workspaces/${currentWorkspace.id}/invoices/${invoice.value.id}`, invoiceRequest);
      emit('invoice-update');
    }

    return { markSent };
  }

  function useInvoiceNavigation({ invoice }) {
    const { navigateToView } = useNavigation();

    function navigateToInvoiceEdit() {
      navigateToView({
        name: 'edit-invoice',
        params: { id: invoice.value.id },
      });
    }

    function markPaid() {
      navigateToView({
        name: 'edit-income',
        params: { invoice: invoice.value },
      });
    }

    return {
      markPaid,
      navigateToInvoiceEdit,
    };
  }

  function useStatus({ invoice }) {
    const status = reactive({
      isDraft: false,
      isPaid: false,
      isCancelled: false,
      isSent: false,
      isOverdue: false,
      statusIcon: null,
      statusValue: null,
      statusText: null,
    });

    function reset() {
      status.isDraft = false;
      status.isPaid = false;
      status.isCancelled = false;
      status.isSent = false;
      status.isOverdue = false;
      status.statusIcon = null;
      status.statusValue = null;
      status.statusText = null;
    }

    // computed does not work for some unclear reason
    watch(invoice, () => {
      reset();
      if (invoice.value.status === 'DRAFT') {
        status.isDraft = true;
        status.statusIcon = 'draft';
        status.statusValue = 'regular';
        status.statusText = i18n.t('invoicesOverviewPanel.status.draft');
      } else if (invoice.value.status === 'PAID') {
        status.isPaid = true;
        status.statusValue = 'success';
        status.statusText = i18n.t('invoicesOverviewPanel.status.finalized');
      } else if (invoice.value.status === 'CANCELLED') {
        status.isCancelled = true;
        status.statusIcon = 'cancel';
        status.statusValue = 'regular';
        status.statusText = i18n.t('invoicesOverviewPanel.status.cancelled');
      } else if (invoice.value.status === 'SENT') {
        status.isSent = true;
        status.statusValue = 'pending';
        status.statusText = i18n.t('invoicesOverviewPanel.status.sent');
      } else if (invoice.value.status === 'OVERDUE') {
        status.isOverdue = true;
        status.statusValue = 'failure';
        status.statusText = i18n.t('invoicesOverviewPanel.status.overdue');
      }
    });
    return { ...toRefs(status) };
  }

  export default {
    components: {
      SaGeneralTaxOutput,
      SaCustomerOutput,
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

    props: {
      invoice: {
        type: Object,
        required: true,
      },
    },

    setup(props, { emit }) {
      const { invoice } = toRefs(props);
      const { generalTaxById } = useGeneralTaxes();
      const { currentWorkspace, defaultCurrency } = useCurrentWorkspace();
      const isGeneralTaxApplicable = computed(() => invoice.value.generalTax != null);
      const isForeignCurrency = computed(() => invoice.value.currency !== defaultCurrency);
      const generalTaxRate = computed(() => generalTaxById.value(invoice.value.generalTax).rateInBps);

      return {
        generalTaxById,
        currentWorkspace,
        ...useInvoiceApi({
          invoice,
          currentWorkspace,
          emit,
        }),
        ...useInvoiceNavigation({ invoice }),
        isGeneralTaxApplicable,
        isForeignCurrency,
        generalTaxRate,
        ...useStatus({ invoice }),
      };
    },
  };
</script>
