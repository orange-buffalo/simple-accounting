<template>
  <div>
    <div class="sa-page-header">
      <h1>{{ pageHeader }}</h1>
    </div>

    <SaForm
      ref="form"
      :loading="loading"
      :model="taxPayment"
      :rules="taxPaymentValidationRules"
    >
      <template #default>
        <div class="row">
          <div class="col col-xs-12 col-lg-6">
            <h2>{{ $t('editIncomeTaxPayment.generalInformation.header') }}</h2>

            <ElFormItem
              :label="$t('editIncomeTaxPayment.generalInformation.title.label')"
              prop="title"
            >
              <ElInput
                v-model="taxPayment.title"
                :placeholder="$t('editIncomeTaxPayment.generalInformation.title.placeholder')"
              />
            </ElFormItem>

            <ElFormItem
              :label="$t('editIncomeTaxPayment.generalInformation.amount.label')"
              prop="amount"
            >
              <MoneyInput
                v-model="taxPayment.amount"
                :currency="defaultCurrency"
              />
            </ElFormItem>

            <ElFormItem
              :label="$t('editIncomeTaxPayment.generalInformation.datePaid.label')"
              prop="datePaid"
            >
              <!-- todo #78: format from cldr https://github.com/ElemeFE/element/issues/11353 -->
              <ElDatePicker
                v-model="taxPayment.datePaid"
                type="date"
                :placeholder="$t('editIncomeTaxPayment.generalInformation.datePaid.placeholder')"
                value-format="yyyy-MM-dd"
              />
            </ElFormItem>

            <ElFormItem
              :label="$t('editIncomeTaxPayment.generalInformation.reportingDate.label')"
              prop="reportingDate"
            >
              <!-- todo #78: format from cldr https://github.com/ElemeFE/element/issues/11353 -->
              <ElDatePicker
                v-model="taxPayment.reportingDate"
                type="date"
                :placeholder="$t('editIncomeTaxPayment.generalInformation.reportingDate.placeholder')"
                value-format="yyyy-MM-dd"
              />
            </ElFormItem>
          </div>

          <div class="col col-xs-12 col-lg-6">
            <h2>{{ $t('editIncomeTaxPayment.additionalInformation.header') }}</h2>

            <ElFormItem
              :label="$t('editIncomeTaxPayment.additionalInformation.notes.label')"
              prop="notes"
            >
              <SaNotesInput
                v-model="taxPayment.notes"
                :placeholder="$t('editIncomeTaxPayment.additionalInformation.notes.placeholder')"
              />
            </ElFormItem>

            <h2>{{ $t('editIncomeTaxPayment.attachments.header') }}</h2>

            <ElFormItem>
              <SaDocumentsUpload
                ref="documentsUpload"
                :documents-ids="taxPayment.attachments"
                :loading-on-create="taxPayment.id != null"
                @uploads-completed="saveTaxPayment"
                @uploads-failed="onDocumentsUploadFailure"
              />
            </ElFormItem>
          </div>
        </div>
      </template>

      <template #buttons-bar>
        <ElButton @click="navigateToTaxPaymentsOverview">
          {{ $t('editIncomeTaxPayment.cancel') }}
        </ElButton>
        <ElButton
          type="primary"
          @click="submitForm"
        >
          {{ $t('editIncomeTaxPayment.save') }}
        </ElButton>
      </template>
    </SaForm>
  </div>
</template>

<script>
  import { reactive } from '@vue/composition-api';
  import i18n from '@/services/i18n';
  import MoneyInput from '@/components/MoneyInput';
  import SaDocumentsUpload from '@/components/documents/SaDocumentsUpload';
  import SaNotesInput from '@/components/SaNotesInput';
  import SaForm from '@/components/SaForm';
  import useCurrentWorkspace from '@/components/workspace/useCurrentWorkspace';
  import useDocumentsUpload from '@/components/documents/useDocumentsUpload';
  import { useLoading } from '@/components/utils/utils';
  import useNavigation from '@/components/navigation/useNavigation';
  import { useApiCrud } from '@/components/utils/api-utils';

  function useTaxPaymentForm(loading) {
    const taxPaymentValidationRules = {
      title: {
        required: true,
        message: i18n.t('editIncomeTaxPayment.validations.title'),
      },
      datePaid: {
        required: true,
        message: i18n.t('editIncomeTaxPayment.validations.datePaid'),
      },
      amount: {
        required: true,
        message: i18n.t('editIncomeTaxPayment.validations.amount'),
      },
    };

    return {
      taxPaymentValidationRules,
      ...useDocumentsUpload(loading),
    };
  }

  function useTaxPaymentApi(taxPayment) {
    const { loading, saveEntity, loadEntity } = useApiCrud({
      apiEntityPath: 'income-tax-payments',
      entity: taxPayment,
      ...useLoading(),
    });

    const saveTaxPayment = async (attachments) => {
      await saveEntity({
        ...taxPayment,
        attachments,
      });
      await navigateToTaxPaymentsOverview();
    };

    loadEntity();

    return {
      loading,
      saveTaxPayment,
    };
  }

  function navigateToTaxPaymentsOverview() {
    const { navigateByViewName } = useNavigation();
    navigateByViewName('income-tax-payments-overview');
  }

  export default {
    components: {
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

      const taxPayment = reactive({
        id,
        attachments: [],
        datePaid: new Date(),
      });

      const pageHeader = id
        ? i18n.t('editIncomeTaxPayment.header.edit')
        : i18n.t('editIncomeTaxPayment.header.create');

      const { loading, saveTaxPayment } = useTaxPaymentApi(taxPayment);

      return {
        taxPayment,
        defaultCurrency,
        ...useTaxPaymentForm(loading),
        pageHeader,
        loading,
        saveTaxPayment,
        navigateToTaxPaymentsOverview,
      };
    },
  };
</script>
