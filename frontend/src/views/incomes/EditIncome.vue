<template>
  <div>
    <div class="sa-page-header">
      <h1>{{ pageHeader }}</h1>
    </div>

    <SaForm
      ref="form"
      :loading="loading"
      :model="income"
      :rules="incomeValidationRules"
    >
      <template #default>
        <div class="row">
          <div class="col col-xs-12 col-lg-6">
            <h2>{{ $t('editIncome.generalInformation.header') }}</h2>

            <ElFormItem
              :label="$t('editIncome.generalInformation.category.label')"
              prop="category"
            >
              <SaCategoryInput
                v-model="income.category"
                :placeholder="$t('editIncome.generalInformation.category.placeholder')"
              />
            </ElFormItem>

            <ElFormItem
              :label="$t('editIncome.generalInformation.title.label')"
              prop="title"
            >
              <ElInput
                v-model="income.title"
                :placeholder="$t('editIncome.generalInformation.title.placeholder')"
              />
            </ElFormItem>

            <ElFormItem
              :label="$t('editIncome.generalInformation.currency.label')"
              prop="currency"
            >
              <SaCurrencyInput v-model="income.currency" />
            </ElFormItem>

            <ElFormItem
              :label="$t('editIncome.generalInformation.originalAmount.label')"
              prop="originalAmount"
            >
              <MoneyInput
                v-model="income.originalAmount"
                :currency="income.currency"
              />
            </ElFormItem>

            <ElFormItem
              :label="$t('editIncome.generalInformation.dateReceived.label')"
              prop="dateReceived"
            >
              <!-- todo #78: format from cldr https://github.com/ElemeFE/element/issues/11353 -->
              <ElDatePicker
                v-model="income.dateReceived"
                type="date"
                :placeholder="$t('editIncome.generalInformation.dateReceived.placeholder')"
                value-format="yyyy-MM-dd"
              />
            </ElFormItem>

            <ElFormItem
              v-if="isInForeignCurrency"
              :label="$t('editIncome.generalInformation.convertedAmountInDefaultCurrency.label', [defaultCurrency])"
              prop="convertedAmountInDefaultCurrency"
            >
              <MoneyInput
                v-model="income.convertedAmountInDefaultCurrency"
                :currency="defaultCurrency"
              />
            </ElFormItem>

            <ElFormItem v-if="isInForeignCurrency">
              <ElCheckbox v-model="income.useDifferentExchangeRateForIncomeTaxPurposes">
                {{ $t('editIncome.generalInformation.useDifferentExchangeRateForIncomeTaxPurposes.label') }}
              </ElCheckbox>
            </ElFormItem>

            <ElFormItem
              v-if="income.useDifferentExchangeRateForIncomeTaxPurposes"
              :label="$t('editIncome.generalInformation.incomeTaxableAmountInDefaultCurrency.label', [defaultCurrency])"
              prop="incomeTaxableAmountInDefaultCurrency"
            >
              <MoneyInput
                v-model="income.incomeTaxableAmountInDefaultCurrency"
                :currency="defaultCurrency"
              />
            </ElFormItem>

            <ElFormItem
              :label="$t('editIncome.generalInformation.generalTax.label')"
              prop="generalTax"
            >
              <SaGeneralTaxInput
                v-model="income.generalTax"
                clearable
                :placeholder="$t('editIncome.generalInformation.generalTax.placeholder')"
              />
            </ElFormItem>
          </div>

          <div class="col col-xs-12 col-lg-6">
            <h2>{{ $t('editIncome.additionalInformation.header') }}</h2>

            <ElFormItem
              v-if="income.linkedInvoice"
              :label="$t('editIncome.additionalInformation.linkedInvoice.label')"
              prop="reportedAmountInDefaultCurrency"
            >
              <span>{{ income.linkedInvoice.title }}</span>
            </ElFormItem>

            <ElFormItem
              :label="$t('editIncome.additionalInformation.notes.label')"
              prop="notes"
            >
              <SaNotesInput
                v-model="income.notes"
                :placeholder="$t('editIncome.additionalInformation.notes.placeholder')"
              />
            </ElFormItem>

            <h2>{{ $t('editIncome.attachments.header') }}</h2>

            <ElFormItem>
              <SaDocumentsUpload
                ref="documentsUpload"
                :documents-ids="income.attachments"
                @uploads-completed="saveIncome"
                @uploads-failed="onDocumentsUploadFailure"
              />
            </ElFormItem>
          </div>
        </div>
      </template>

      <template #buttons-bar>
        <ElButton @click="navigateToIncomesOverview">
          {{ $t('editIncome.cancel') }}
        </ElButton>
        <ElButton
          type="primary"
          @click="submitForm"
        >
          {{ $t('editIncome.save') }}
        </ElButton>
      </template>
    </SaForm>
  </div>
</template>

<script>
  import { computed, reactive } from '@vue/composition-api';
  import MoneyInput from '@/components/MoneyInput';
  import SaCurrencyInput from '@/components/SaCurrencyInput';
  import SaDocumentsUpload from '@/components/documents/SaDocumentsUpload';
  import SaNotesInput from '@/components/SaNotesInput';
  import SaForm from '@/components/SaForm';
  import SaCategoryInput from '@/components/category/SaCategoryInput';
  import SaGeneralTaxInput from '@/components/general-tax/SaGeneralTaxInput';
  import useNavigation from '@/components/navigation/useNavigation';
  import i18n from '@/services/i18n';
  import useCurrentWorkspace from '@/components/workspace/useCurrentWorkspace';
  import useDocumentsUpload from '@/components/documents/useDocumentsUpload';
  import { safeAssign, useLoading } from '@/components/utils/utils';
  import { useApiCrud } from '@/components/utils/api-utils';

  async function navigateToIncomesOverview() {
    const { navigateByViewName } = useNavigation();
    await navigateByViewName('incomes-overview');
  }

  function useIncomeApi(income) {
    const { loading, saveEntity, loadEntity } = useApiCrud({
      apiEntityPath: 'incomes',
      entity: income,
      ...useLoading(),
    });

    const saveIncome = async (attachments) => {
      await saveEntity({
        ...income,
        attachments,
      });
      await navigateToIncomesOverview();
    };

    loadEntity((incomeResponse) => {
      const {
        convertedAmounts,
        incomeTaxableAmounts,
        generalTaxRateInBps,
        generalTaxAmount,
        status,
        version,
        timeRecorded,
        ...incomeEditProperties
      } = incomeResponse;
      safeAssign(income, {
        ...incomeEditProperties,
        convertedAmountInDefaultCurrency: convertedAmounts.originalAmountInDefaultCurrency,
        incomeTaxableAmountInDefaultCurrency: incomeTaxableAmounts.originalAmountInDefaultCurrency,
      });
    });

    return {
      loading,
      saveIncome,
    };
  }

  function useIncomeForm(loading) {
    const incomeValidationRules = {
      currency: {
        required: true,
        message: i18n.t('editIncome.validations.currency'),
      },
      title: {
        required: true,
        message: i18n.t('editIncome.validations.title'),
      },
      dateReceived: {
        required: true,
        message: i18n.t('editIncome.validations.dateReceived'),
      },
      originalAmount: {
        required: true,
        message: i18n.t('editIncome.validations.originalAmount'),
      },
    };
    return {
      incomeValidationRules,
      ...useDocumentsUpload(loading),
    };
  }

  export default {
    components: {
      SaGeneralTaxInput,
      SaCategoryInput,
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

      const income = reactive({
        id,
        currency: defaultCurrency,
        useDifferentExchangeRateForIncomeTaxPurposes: false,
        attachments: [],
        dateReceived: new Date(),
      });

      const isInForeignCurrency = computed(() => income.currency !== defaultCurrency);

      const pageHeader = id ? i18n.t('editIncome.pageHeader.edit') : i18n.t('editIncome.pageHeader.create');

      const { loading, saveIncome } = useIncomeApi(income);

      return {
        income,
        isInForeignCurrency,
        defaultCurrency,
        navigateToIncomesOverview,
        pageHeader,
        ...useIncomeForm(loading),
        loading,
        saveIncome,
      };
    },
  };
</script>
