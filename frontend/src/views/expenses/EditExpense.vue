<template>
  <div>
    <div class="sa-page-header">
      <h1>{{ pageHeader }}</h1>
    </div>

    <SaForm
      ref="form"
      :loading="loading"
      :model="expense"
      :rules="expenseValidationRules"
    >
      <template #default>
        <div class="row">
          <div class="col col-xs-12 col-lg-6">
            <h2>{{ $t('editExpense.generalInformation.header') }}</h2>

            <ElFormItem
              :label="$t('editExpense.generalInformation.category.label')"
              prop="category"
            >
              <SaCategoryInput
                v-model="expense.category"
                :placeholder="$t('editExpense.generalInformation.category.placeholder')"
              />
            </ElFormItem>

            <ElFormItem
              :label="$t('editExpense.generalInformation.title.label')"
              prop="title"
            >
              <ElInput
                v-model="expense.title"
                :placeholder="$t('editExpense.generalInformation.title.placeholder')"
              />
            </ElFormItem>

            <ElFormItem
              :label="$t('editExpense.generalInformation.currency.label')"
              prop="currency"
            >
              <SaCurrencyInput v-model="expense.currency" />
            </ElFormItem>

            <ElFormItem
              :label="$t('editExpense.generalInformation.originalAmount.label')"
              prop="originalAmount"
            >
              <MoneyInput
                v-model="expense.originalAmount"
                :currency="expense.currency"
              />
            </ElFormItem>

            <ElFormItem
              :label="$t('editExpense.generalInformation.datePaid.label')"
              prop="datePaid"
            >
              <!-- todo #78: format from cldr https://github.com/ElemeFE/element/issues/11353 -->
              <ElDatePicker
                v-model="expense.datePaid"
                type="date"
                :placeholder="$t('editExpense.generalInformation.datePaid.placeholder')"
                value-format="yyyy-MM-dd"
              />
            </ElFormItem>

            <ElFormItem
              v-if="isInForeignCurrency"
              :label="$t('editExpense.generalInformation.convertedAmountInDefaultCurrency.label', [defaultCurrency])"
              prop="convertedAmountInDefaultCurrency"
            >
              <MoneyInput
                v-model="expense.convertedAmountInDefaultCurrency"
                :currency="defaultCurrency"
              />
            </ElFormItem>

            <ElFormItem v-if="isInForeignCurrency">
              <ElCheckbox v-model="expense.useDifferentExchangeRateForIncomeTaxPurposes">
                {{ $t('editExpense.generalInformation.useDifferentExchangeRateForIncomeTaxPurposes.label') }}
              </ElCheckbox>
            </ElFormItem>

            <!-- eslint-disable max-len-->
            <ElFormItem
              v-if="isInForeignCurrency && expense.useDifferentExchangeRateForIncomeTaxPurposes"
              :label="$t('editExpense.generalInformation.incomeTaxableAmountInDefaultCurrency.label', [defaultCurrency])"
              prop="incomeTaxableAmountInDefaultCurrency"
            >
              <MoneyInput
                v-model="expense.incomeTaxableAmountInDefaultCurrency"
                :currency="defaultCurrency"
              />
            </ElFormItem>

            <ElFormItem
              :label="$t('editExpense.generalInformation.generalTax.label')"
              prop="generalTax"
            >
              <SaGeneralTaxInput
                v-model="expense.generalTax"
                clearable
                :placeholder="$t('editExpense.generalInformation.generalTax.placeholder')"
              />
            </ElFormItem>

            <ElFormItem>
              <ElCheckbox v-model="partialForBusiness">
                {{ $t('editExpense.generalInformation.partialForBusiness.label') }}
              </ElCheckbox>
            </ElFormItem>

            <ElFormItem
              v-if="partialForBusiness"
              :label="$t('editExpense.generalInformation.percentOnBusiness.label')"
              prop="percentOnBusiness"
            >
              <ElInputNumber
                v-model="expense.percentOnBusiness"
                :min="0"
                :max="100"
              />
            </ElFormItem>
          </div>

          <div class="col col-xs-12 col-lg-6">
            <h2>{{ $t('editExpense.additionalInformation.header') }}</h2>

            <ElFormItem
              :label="$t('editExpense.additionalInformation.notes.label')"
              prop="notes"
            >
              <SaNotesInput
                v-model="expense.notes"
                :placeholder="$t('editExpense.additionalInformation.notes.placeholder')"
              />
            </ElFormItem>

            <h2>{{ $t('editExpense.attachments.header') }}</h2>

            <ElFormItem>
              <SaDocumentsUpload
                ref="documentsUpload"
                :documents-ids="expense.attachments"
                @uploads-completed="saveExpense"
                @uploads-failed="onDocumentsUploadFailure"
              />
            </ElFormItem>
          </div>
        </div>
      </template>

      <template #buttons-bar>
        <ElButton @click="navigateToExpensesOverview">
          {{ $t('editExpense.cancel') }}
        </ElButton>
        <ElButton
          type="primary"
          @click="submitForm"
        >
          {{ $t('editExpense.save') }}
        </ElButton>
      </template>
    </SaForm>
  </div>
</template>

<script>
  import { computed, reactive, toRefs } from '@vue/composition-api';
  import { api } from '@/services/api';
  import MoneyInput from '@/components/MoneyInput';
  import SaCurrencyInput from '@/components/SaCurrencyInput';
  import SaDocumentsUpload from '@/components/documents/SaDocumentsUpload';
  import SaNotesInput from '@/components/SaNotesInput';
  import SaForm from '@/components/SaForm';
  import SaCategoryInput from '@/components/category/SaCategoryInput';
  import SaGeneralTaxInput from '@/components/general-tax/SaGeneralTaxInput';
  import i18n from '@/services/i18n';
  import useCurrentWorkspace from '@/components/workspace/useCurrentWorkspace';
  import { safeAssign, useLoading } from '@/components/utils/utils';
  import useNavigation from '@/components/navigation/useNavigation';
  import useDocumentsUpload from '@/components/documents/useDocumentsUpload';

  function copyExpenseProperties(targetExpense, sourceExpense, overrides) {
    const {
      convertedAmounts,
      incomeTaxableAmounts,
      generalTaxRateInBps,
      generalTaxAmount,
      status,
      version,
      timeRecorded,
      ...expenseEditProperties
    } = sourceExpense;
    safeAssign(targetExpense, {
      ...expenseEditProperties,
      convertedAmountInDefaultCurrency: convertedAmounts.originalAmountInDefaultCurrency,
      incomeTaxableAmountInDefaultCurrency: incomeTaxableAmounts.originalAmountInDefaultCurrency,
      ...overrides,
    });
  }

  function setupUiState(expense, uiState) {
    // eslint-disable-next-line no-param-reassign
    uiState.partialForBusiness = expense.percentOnBusiness !== 100;
  }

  async function loadExpense(expense, uiState, withLoading) {
    withLoading(async () => {
      const { currentWorkspaceApiUrl } = useCurrentWorkspace();
      const expenseResponse = await api.get(currentWorkspaceApiUrl(`expenses/${expense.id}`));
      copyExpenseProperties(expense, expenseResponse.data);
      setupUiState(expense, uiState);
    });
  }

  function submitExpense(withLoading, expense, uiState, documentsIds) {
    return withLoading(async () => {
      const {
        id,
        ...expensePropertiesToPush
      } = expense;

      const expenseToPush = {
        ...expensePropertiesToPush,
        percentOnBusiness: uiState.partialForBusiness ? expense.percentOnBusiness : null,
        attachments: documentsIds,
      };

      const { currentWorkspaceApiUrl } = useCurrentWorkspace();
      if (expense.id) {
        await api.put(currentWorkspaceApiUrl(`/expenses/${expense.id}`), expenseToPush);
      } else {
        await api.post(currentWorkspaceApiUrl('expenses'), expenseToPush);
      }

      await navigateToExpensesOverview();
    });
  }

  function useExpenseApi(expense, uiState) {
    const { loading, withLoading } = useLoading();

    if (expense.id) {
      loadExpense(expense, uiState, withLoading);
    }

    const saveExpense = documentsIds => submitExpense(withLoading, expense, uiState, documentsIds);

    return {
      saveExpense,
      loading,
    };
  }

  function useExpenseForm(loading) {
    const expenseValidationRules = {
      currency: {
        required: true,
        message: i18n.t('editExpense.validations.currency'),
      },
      title: {
        required: true,
        message: i18n.t('editExpense.validations.title'),
      },
      datePaid: {
        required: true,
        message: i18n.t('editExpense.validations.datePaid'),
      },
      originalAmount: {
        required: true,
        message: i18n.t('editExpense.validations.originalAmount'),
      },
    };

    return {
      expenseValidationRules,
      ...useDocumentsUpload(loading),
    };
  }

  async function navigateToExpensesOverview() {
    const { navigateByViewName } = useNavigation();
    await navigateByViewName('expenses-overview');
  }

  export default {
    components: {
      SaGeneralTaxInput,
      SaCurrencyInput,
      SaForm,
      SaNotesInput,
      SaDocumentsUpload,
      MoneyInput,
      SaCategoryInput,
    },

    props: {
      id: {
        type: Number,
        default: null,
      },
      prototype: {
        type: Object,
        default: null,
      },
    },

    setup({ id, prototype }) {
      const { defaultCurrency } = useCurrentWorkspace();

      const expense = reactive({
        attachments: [],
        percentOnBusiness: 100,
        datePaid: new Date(),
        currency: defaultCurrency,
        id,
      });

      const isInForeignCurrency = computed(() => expense.currency !== defaultCurrency);

      const uiState = reactive({
        partialForBusiness: false,
      });

      const pageHeader = id ? i18n.t('editExpense.pageHeader.edit') : i18n.t('editExpense.pageHeader.create');

      if (prototype) {
        copyExpenseProperties(expense, prototype, {
          datePaid: null,
          id: null,
        });
        setupUiState(expense, uiState);
      }

      const { loading, saveExpense } = useExpenseApi(expense, uiState);

      return {
        expense,
        defaultCurrency,
        isInForeignCurrency,
        ...toRefs(uiState),
        pageHeader,
        navigateToExpensesOverview,
        ...useExpenseForm(loading),
        loading,
        saveExpense,
      };
    },
  };
</script>
