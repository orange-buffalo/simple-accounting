<template>
  <div>
    <div class="sa-page-header">
      <h1>{{ pageHeader }}</h1>

      <div
        v-if="isEditing"
        class="sa-header-options"
      >
        <span />
        <ElButton
          v-if="invoice.status !== 'CANCELLED'"
          type="danger"
          @click="cancelInvoice"
        >
          {{ $t('editInvoice.cancelInvoice.button') }}
        </ElButton>
        <SaStatusLabel
          v-if="invoice.status === 'CANCELLED'"
          status="failure"
        >
          {{ $t('editInvoice.cancelInvoice.status') }}
        </SaStatusLabel>
      </div>
    </div>

    <SaForm
      ref="form"
      :loading="loading"
      :model="invoice"
      :rules="invoiceValidationRules"
    >
      <template #default>
        <div class="row">
          <div class="col col-xs-12 col-lg-6">
            <h2>{{ $t('editInvoice.generalInformation.header') }}</h2>

            <ElFormItem
              :label="$t('editInvoice.generalInformation.customer.label')"
              prop="customer"
            >
              <SaCustomerInput
                v-model="invoice.customer"
                :placeholder="$t('editInvoice.generalInformation.customer.placeholder')"
              />
            </ElFormItem>

            <ElFormItem
              :label="$t('editInvoice.generalInformation.title.label')"
              prop="title"
            >
              <ElInput
                v-model="invoice.title"
                :placeholder="$t('editInvoice.generalInformation.title.placeholder')"
              />
            </ElFormItem>

            <ElFormItem
              :label="$t('editInvoice.generalInformation.currency.label')"
              prop="currency"
            >
              <SaCurrencyInput v-model="invoice.currency" />
            </ElFormItem>

            <ElFormItem
              :label="$t('editInvoice.generalInformation.amount.label')"
              prop="amount"
            >
              <MoneyInput
                v-model="invoice.amount"
                :currency="invoice.currency"
              />
            </ElFormItem>

            <ElFormItem
              :label="$t('editInvoice.generalInformation.dateIssued.label')"
              prop="dateIssued"
            >
              <!-- todo #78: format from cldr https://github.com/ElemeFE/element/issues/11353 -->
              <ElDatePicker
                v-model="invoice.dateIssued"
                type="date"
                :placeholder="$t('editInvoice.generalInformation.dateIssued.placeholder')"
                value-format="yyyy-MM-dd"
              />
            </ElFormItem>

            <ElFormItem
              :label="$t('editInvoice.generalInformation.dueDate.label')"
              prop="dueDate"
            >
              <!-- todo #78: format from cldr https://github.com/ElemeFE/element/issues/11353 -->
              <ElDatePicker
                v-model="invoice.dueDate"
                type="date"
                :placeholder="$t('editInvoice.generalInformation.dueDate.placeholder')"
                value-format="yyyy-MM-dd"
              />
            </ElFormItem>

            <ElFormItem
              :label="$t('editInvoice.generalInformation.generalTax.label')"
              prop="generalTax"
            >
              <SaGeneralTaxInput
                v-model="invoice.generalTax"
                clearable
                :placeholder="$t('editInvoice.generalInformation.generalTax.placeholder')"
              />
            </ElFormItem>

            <ElFormItem>
              <ElCheckbox v-model="alreadySent">
                {{ $t('editInvoice.generalInformation.alreadySent.label') }}
              </ElCheckbox>
            </ElFormItem>

            <ElFormItem
              v-if="alreadySent"
              :label="$t('editInvoice.generalInformation.dateSent.label')"
              prop="dateSent"
            >
              <!-- todo #78: format from cldr https://github.com/ElemeFE/element/issues/11353 -->
              <ElDatePicker
                v-model="invoice.dateSent"
                type="date"
                :placeholder="$t('editInvoice.generalInformation.dateSent.placeholder')"
                value-format="yyyy-MM-dd"
              />
            </ElFormItem>

            <ElFormItem>
              <ElCheckbox v-model="alreadyPaid">
                {{ $t('editInvoice.generalInformation.alreadyPaid.label') }}
              </ElCheckbox>
            </ElFormItem>

            <ElFormItem
              v-if="alreadyPaid"
              :label="$t('editInvoice.generalInformation.datePaid.label')"
              prop="datePaid"
            >
              <!-- todo #78: format from cldr https://github.com/ElemeFE/element/issues/11353 -->
              <ElDatePicker
                v-model="invoice.datePaid"
                type="date"
                :placeholder="$t('editInvoice.generalInformation.datePaid.placeholder')"
                value-format="yyyy-MM-dd"
              />
            </ElFormItem>
          </div>

          <div class="col col-xs-12 col-lg-6">
            <h2>{{ $t('editInvoice.additionalNotes.header') }}</h2>

            <ElFormItem
              :label="$t('editInvoice.additionalNotes.notes.label')"
              prop="notes"
            >
              <SaNotesInput
                v-model="invoice.notes"
                :placeholder="$t('editInvoice.additionalNotes.notes.placeholder')"
              />
            </ElFormItem>

            <h2>{{ $t('editInvoice.attachments.header') }}</h2>

            <ElFormItem>
              <SaDocumentsUpload
                ref="documentsUpload"
                :documents-ids="invoice.attachments"
                :loading-on-create="invoice.id != null"
                @uploads-completed="saveInvoice"
                @uploads-failed="onDocumentsUploadFailure"
              />
            </ElFormItem>
          </div>
        </div>
      </template>

      <template #buttons-bar>
        <ElButton @click="navigateToInvoicesOverview">
          {{ $t('editInvoice.cancel') }}
        </ElButton>
        <ElButton
          type="primary"
          @click="submitForm"
        >
          {{ $t('editInvoice.save') }}
        </ElButton>
      </template>
    </SaForm>
  </div>
</template>

<script>
  import { reactive, toRefs } from '@vue/composition-api';
  import { api } from '@/services/api';
  import i18n from '@/services/i18n';
  import MoneyInput from '@/components/MoneyInput';
  import SaCurrencyInput from '@/components/SaCurrencyInput';
  import SaDocumentsUpload from '@/components/documents/SaDocumentsUpload';
  import SaNotesInput from '@/components/SaNotesInput';
  import SaForm from '@/components/SaForm';
  import SaCustomerInput from '@/components/customer/SaCustomerInput';
  import SaGeneralTaxInput from '@/components/general-tax/SaGeneralTaxInput';
  import useCurrentWorkspace from '@/components/workspace/useCurrentWorkspace';
  import { safeAssign, useConfirmation, useLoading } from '@/components/utils/utils';
  import useNavigation from '@/components/navigation/useNavigation';
  import useDocumentsUpload from '@/components/documents/useDocumentsUpload';
  import { useApiCrud } from '@/components/utils/api-utils';
  import SaStatusLabel from '@/components/SaStatusLabel';

  function useInvoiceApi(invoice, uiState) {
    const {
      loadEntity,
      loading,
      saveEntity,
      withLoadingProducer,
    } = useApiCrud({
      apiEntityPath: 'invoices',
      entity: invoice,
      ...useLoading(),
    });

    const saveInvoice = async (attachments) => {
      await saveEntity({
        customer: invoice.customer,
        dateIssued: invoice.dateIssued,
        datePaid: uiState.alreadyPaid ? invoice.datePaid : null,
        dateSent: uiState.alreadySent ? invoice.dateSent : null,
        dueDate: invoice.dueDate,
        title: invoice.title,
        currency: invoice.currency,
        amount: invoice.amount,
        attachments,
        notes: invoice.notes,
        generalTax: invoice.generalTax,
      });
      await navigateToInvoicesOverview();
    };

    const { currentWorkspaceId } = useCurrentWorkspace();

    const cancelInvoice = withLoadingProducer(async () => {
      const { data } = await api.post(`workspaces/${currentWorkspaceId}/invoices/${invoice.id}/cancel`);
      safeAssign(invoice, data);
    });

    loadEntity((invoiceResponse) => {
      safeAssign(invoice, invoiceResponse);
      safeAssign(uiState, {
        alreadyPaid: invoice.datePaid != null,
        alreadySent: invoice.dateSent != null,
      });
    });

    return {
      loading,
      saveInvoice,
      cancelInvoice,
    };
  }

  function useInvoiceForm(loading) {
    const invoiceValidationRules = {
      customer: {
        required: true,
        message: i18n.t('editInvoice.validations.customer'),
      },
      currency: {
        required: true,
        message: i18n.t('editInvoice.validations.currency'),
      },
      title: {
        required: true,
        message: i18n.t('editInvoice.validations.title'),
      },
      amount: {
        required: true,
        message: i18n.t('editInvoice.validations.amount'),
      },
      dateIssued: {
        required: true,
        message: i18n.t('editInvoice.validations.dateIssued'),
      },
      dueDate: {
        required: true,
        message: i18n.t('editInvoice.validations.dueDate'),
      },
      dateSent: {
        required: true,
        message: i18n.t('editInvoice.validations.dateSent'),
      },
      datePaid: {
        required: true,
        message: i18n.t('editInvoice.validations.datePaid'),
      },
    };
    return {
      ...useDocumentsUpload(loading),
      invoiceValidationRules,
    };
  }

  async function navigateToInvoicesOverview() {
    const { navigateByViewName } = useNavigation();
    await navigateByViewName('invoices-overview');
  }

  export default {
    components: {
      SaStatusLabel,
      SaGeneralTaxInput,
      SaCustomerInput,
      SaCurrencyInput,
      SaForm,
      SaNotesInput,
      SaDocumentsUpload,
      MoneyInput,
    },

    props: {
      id: {
        type: Number,
        default: null,
      },
    },

    setup({ id }) {
      const { defaultCurrency } = useCurrentWorkspace();

      const invoice = reactive({
        id,
        attachments: [],
        dateIssued: new Date(),
        currency: defaultCurrency,
      });

      const uiState = reactive({
        alreadySent: false,
        alreadyPaid: false,
        isEditing: id != null,
      });

      const pageHeader = id ? i18n.t('editInvoice.pageHeader.edit') : i18n.t('editInvoice.pageHeader.create');

      const { loading, saveInvoice, cancelInvoice } = useInvoiceApi(invoice, uiState);

      const { executeAfterConfirmation } = useConfirmation();
      const cancelInvoiceWithConfirmation = () => executeAfterConfirmation(
        i18n.t('editInvoice.cancelInvoice.confirm.message'),
        {
          title: 'Warning',
          confirmButtonText: i18n.t('editInvoice.cancelInvoice.confirm.yes'),
          cancelButtonText: i18n.t('editInvoice.cancelInvoice.confirm.no'),
          type: 'warning',
        },
        cancelInvoice,
      );

      return {
        invoice,
        defaultCurrency,
        ...toRefs(uiState),
        ...useInvoiceForm(loading),
        pageHeader,
        navigateToInvoicesOverview,
        loading,
        saveInvoice,
        cancelInvoice: cancelInvoiceWithConfirmation,
      };
    },
  };
</script>
