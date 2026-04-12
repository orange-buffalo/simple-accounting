<template>
  <div>
    <div class="sa-page-header">
      <h1>{{ pageHeader }}</h1>
    </div>

    <SaLegacyForm
      ref="formRef"
      :model="expense"
      :rules="expenseValidationRules"
      :initially-loading="id !== undefined || prototype !== undefined"
    >
      <template #default>
        <div class="row">
          <div class="col col-xs-12 col-lg-6">
            <h2>{{ $t.editExpense.generalInformation.header() }}</h2>

            <ElFormItem
              :label="$t.editExpense.generalInformation.category.label()"
              prop="category"
            >
              <SaCategoryInput
                v-model="expense.category"
                :placeholder="$t.editExpense.generalInformation.category.placeholder()"
              />
            </ElFormItem>

            <ElFormItem
              :label="$t.editExpense.generalInformation.title.label()"
              prop="title"
            >
              <ElInput
                v-model="expense.title"
                :placeholder="$t.editExpense.generalInformation.title.placeholder()"
              />
            </ElFormItem>

            <ElFormItem
              :label="$t.editExpense.generalInformation.currency.label()"
              prop="currency"
            >
              <SaCurrencyInput v-model="expense.currency" />
            </ElFormItem>

            <ElFormItem
              :label="$t.editExpense.generalInformation.originalAmount.label()"
              prop="originalAmount"
            >
              <SaMoneyInput
                v-model="expense.originalAmount"
                :currency="expense.currency"
              />
            </ElFormItem>

            <ElFormItem
              :label="$t.editExpense.generalInformation.datePaid.label()"
              prop="datePaid"
            >
              <!-- todo #78: format from cldr https://github.com/ElemeFE/element/issues/11353 -->
              <ElDatePicker
                v-model="expense.datePaid"
                type="date"
                value-format="YYYY-MM-DD"
                :placeholder="$t.editExpense.generalInformation.datePaid.placeholder()"
              />
            </ElFormItem>

            <ElFormItem
              v-if="isInForeignCurrency"
              :label="$t.editExpense.generalInformation.convertedAmountInDefaultCurrency.label(defaultCurrency)"
              prop="convertedAmountInDefaultCurrency"
            >
              <SaMoneyInput
                v-model="expense.convertedAmountInDefaultCurrency"
                :currency="defaultCurrency"
              />
            </ElFormItem>

            <ElFormItem v-if="isInForeignCurrency">
              <ElCheckbox v-model="expense.useDifferentExchangeRateForIncomeTaxPurposes">
                {{ $t.editExpense.generalInformation.useDifferentExchangeRateForIncomeTaxPurposes.label() }}
              </ElCheckbox>
            </ElFormItem>

            <!-- eslint-disable max-len-->
            <ElFormItem
              v-if="isInForeignCurrency && expense.useDifferentExchangeRateForIncomeTaxPurposes"
              :label="$t.editExpense.generalInformation.incomeTaxableAmountInDefaultCurrency.label(defaultCurrency)"
              prop="incomeTaxableAmountInDefaultCurrency"
            >
              <SaMoneyInput
                v-model="expense.incomeTaxableAmountInDefaultCurrency"
                :currency="defaultCurrency"
              />
            </ElFormItem>

            <ElFormItem
              :label="$t.editExpense.generalInformation.generalTax.label()"
              prop="generalTax"
            >
              <SaGeneralTaxInput
                v-model="expense.generalTax"
                clearable
                :placeholder="$t.editExpense.generalInformation.generalTax.placeholder()"
              />
            </ElFormItem>

            <ElFormItem>
              <ElCheckbox v-model="uiState.partialForBusiness">
                {{ $t.editExpense.generalInformation.partialForBusiness.label() }}
              </ElCheckbox>
            </ElFormItem>

            <ElFormItem
              v-if="uiState.partialForBusiness"
              :label="$t.editExpense.generalInformation.percentOnBusiness.label()"
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
            <h2>{{ $t.editExpense.additionalInformation.header() }}</h2>

            <ElFormItem
              :label="$t.editExpense.additionalInformation.notes.label()"
              prop="notes"
            >
              <SaNotesInput
                v-model="expense.notes"
                :placeholder="$t.editExpense.additionalInformation.notes.placeholder()"
              />
            </ElFormItem>

            <h2>{{ $t.editExpense.attachments.header() }}</h2>

            <ElFormItem>
              <SaDocumentsUpload
                ref="documentsUploadRef"
                v-model:documents-ids="expense.attachments"
                :loading-on-create="id !== undefined"
                @uploads-completed="onDocumentsUploadComplete"
                @uploads-failed="onDocumentsUploadFailure"
              />
            </ElFormItem>
          </div>
        </div>
      </template>

      <template #buttons-bar>
        <ElButton @click="navigateToExpensesOverview">
          {{ $t.editExpense.cancel() }}
        </ElButton>
        <ElButton
          type="primary"
          @click="submitForm"
        >
          {{ $t.editExpense.save() }}
        </ElButton>
      </template>
    </SaLegacyForm>
  </div>
</template>

<script lang="ts" setup>
  import { computed, ref } from 'vue';
  import SaMoneyInput from '@/components/SaMoneyInput.vue';
  import SaCurrencyInput from '@/components/currency-input/SaCurrencyInput.vue';
  import SaDocumentsUpload from '@/components/documents/SaDocumentsUpload.vue';
  import SaNotesInput from '@/components/notes-input/SaNotesInput.vue';
  import SaLegacyForm from '@/components/form/SaLegacyForm.vue';
  import SaCategoryInput from '@/components/category/SaCategoryInput.vue';
  import SaGeneralTaxInput from '@/components/general-tax/SaGeneralTaxInput.vue';
  import { $t } from '@/services/i18n';
  import useNavigation from '@/services/use-navigation';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import { useFormWithDocumentsUpload } from '@/components/form/use-form';
  import { formatDateToLocalISOString } from '@/services/date-utils';
  import { graphql } from '@/services/api/gql';
  import { useMutation, useLazyQuery } from '@/services/api/use-gql-api.ts';

  const props = defineProps<{
    id?: number,
    prototype?: string,
  }>();

  const expenseValidationRules = {
    currency: {
      required: true,
      message: $t.value.editExpense.validations.currency(),
    },
    title: {
      required: true,
      message: $t.value.editExpense.validations.title(),
    },
    datePaid: {
      required: true,
      message: $t.value.editExpense.validations.datePaid(),
    },
    originalAmount: {
      required: true,
      message: $t.value.editExpense.validations.originalAmount(),
    },
  };

  const { navigateByViewName } = useNavigation();
  const navigateToExpensesOverview = async () => {
    await navigateByViewName('expenses-overview');
  };

  const {
    currentWorkspaceId,
    defaultCurrency,
  } = useCurrentWorkspace();

  type ExpenseFormValues = {
    category?: number,
    title?: string,
    datePaid?: string,
    currency: string,
    originalAmount?: number,
    convertedAmountInDefaultCurrency?: number,
    useDifferentExchangeRateForIncomeTaxPurposes: boolean,
    incomeTaxableAmountInDefaultCurrency?: number,
    notes?: string,
    percentOnBusiness: number,
    generalTax?: number,
    attachments: number[],
  };

  const expense = ref<ExpenseFormValues>({
    attachments: [],
    percentOnBusiness: 100,
    datePaid: formatDateToLocalISOString(new Date()),
    currency: defaultCurrency,
    useDifferentExchangeRateForIncomeTaxPurposes: false,
  });

  const uiState = ref<{
    partialForBusiness: boolean,
  }>({
    partialForBusiness: false,
  });

  const getExpenseQuery = useLazyQuery(graphql(`
    query getExpenseForEdit($workspaceId: Long!, $expenseId: Long!) {
      workspace(id: $workspaceId) {
        expense(id: $expenseId) {
          id
          category {
            id
          }
          title
          datePaid
          currency
          originalAmount
          convertedAmounts {
            originalAmountInDefaultCurrency
          }
          useDifferentExchangeRateForIncomeTaxPurposes
          incomeTaxableAmounts {
            originalAmountInDefaultCurrency
          }
          notes
          percentOnBusiness
          generalTaxId
          attachments {
            id
          }
        }
      }
    }
  `), 'workspace');

  const loadExpense = async () => {
    if (props.id !== undefined) {
      const workspace = await getExpenseQuery({
        workspaceId: currentWorkspaceId,
        expenseId: props.id,
      });
      const loaded = workspace?.expense;
      if (loaded) {
        expense.value = {
          category: loaded.category?.id ?? undefined,
          title: loaded.title,
          datePaid: loaded.datePaid,
          currency: loaded.currency,
          originalAmount: loaded.originalAmount,
          convertedAmountInDefaultCurrency: loaded.convertedAmounts.originalAmountInDefaultCurrency ?? undefined,
          useDifferentExchangeRateForIncomeTaxPurposes: loaded.useDifferentExchangeRateForIncomeTaxPurposes,
          incomeTaxableAmountInDefaultCurrency: loaded.incomeTaxableAmounts.originalAmountInDefaultCurrency ?? undefined,
          notes: loaded.notes ?? undefined,
          percentOnBusiness: loaded.percentOnBusiness,
          generalTax: loaded.generalTaxId ?? undefined,
          attachments: loaded.attachments.map(a => a.id),
        };
        uiState.value.partialForBusiness = loaded.percentOnBusiness !== 100;
      }
    } else if (props.prototype !== undefined) {
      const workspace = await getExpenseQuery({
        workspaceId: currentWorkspaceId,
        expenseId: Number(props.prototype),
      });
      const prototypeExpense = workspace?.expense;
      if (prototypeExpense) {
        expense.value = {
          category: prototypeExpense.category?.id ?? undefined,
          title: prototypeExpense.title,
          datePaid: undefined,
          currency: prototypeExpense.currency,
          originalAmount: prototypeExpense.originalAmount,
          convertedAmountInDefaultCurrency: undefined,
          useDifferentExchangeRateForIncomeTaxPurposes: prototypeExpense.useDifferentExchangeRateForIncomeTaxPurposes,
          incomeTaxableAmountInDefaultCurrency: undefined,
          notes: prototypeExpense.notes ?? undefined,
          percentOnBusiness: prototypeExpense.percentOnBusiness,
          generalTax: prototypeExpense.generalTaxId ?? undefined,
          attachments: [],
        };
        uiState.value.partialForBusiness = prototypeExpense.percentOnBusiness !== 100;
      }
    }
  };

  const createExpenseMutation = useMutation(graphql(`
    mutation createExpenseMutation(
      $workspaceId: Long!,
      $title: String!,
      $datePaid: LocalDate!,
      $currency: String!,
      $originalAmount: Long!,
      $convertedAmountInDefaultCurrency: Long,
      $useDifferentExchangeRateForIncomeTaxPurposes: Boolean!,
      $incomeTaxableAmountInDefaultCurrency: Long,
      $notes: String,
      $percentOnBusiness: Int,
      $attachments: [Long!],
      $categoryId: Long,
      $generalTaxId: Long
    ) {
      createExpense(
        workspaceId: $workspaceId,
        title: $title,
        datePaid: $datePaid,
        currency: $currency,
        originalAmount: $originalAmount,
        convertedAmountInDefaultCurrency: $convertedAmountInDefaultCurrency,
        useDifferentExchangeRateForIncomeTaxPurposes: $useDifferentExchangeRateForIncomeTaxPurposes,
        incomeTaxableAmountInDefaultCurrency: $incomeTaxableAmountInDefaultCurrency,
        notes: $notes,
        percentOnBusiness: $percentOnBusiness,
        attachments: $attachments,
        categoryId: $categoryId,
        generalTaxId: $generalTaxId
      ) {
        id
      }
    }
  `), 'createExpense');

  const editExpenseMutation = useMutation(graphql(`
    mutation editExpenseMutation(
      $workspaceId: Long!,
      $id: Long!,
      $title: String!,
      $datePaid: LocalDate!,
      $currency: String!,
      $originalAmount: Long!,
      $convertedAmountInDefaultCurrency: Long,
      $useDifferentExchangeRateForIncomeTaxPurposes: Boolean!,
      $incomeTaxableAmountInDefaultCurrency: Long,
      $notes: String,
      $percentOnBusiness: Int,
      $attachments: [Long!],
      $categoryId: Long,
      $generalTaxId: Long
    ) {
      editExpense(
        workspaceId: $workspaceId,
        id: $id,
        title: $title,
        datePaid: $datePaid,
        currency: $currency,
        originalAmount: $originalAmount,
        convertedAmountInDefaultCurrency: $convertedAmountInDefaultCurrency,
        useDifferentExchangeRateForIncomeTaxPurposes: $useDifferentExchangeRateForIncomeTaxPurposes,
        incomeTaxableAmountInDefaultCurrency: $incomeTaxableAmountInDefaultCurrency,
        notes: $notes,
        percentOnBusiness: $percentOnBusiness,
        attachments: $attachments,
        categoryId: $categoryId,
        generalTaxId: $generalTaxId
      ) {
        id
      }
    }
  `), 'editExpense');

  const saveExpense = async () => {
    const percentOnBusiness = uiState.value.partialForBusiness ? expense.value.percentOnBusiness : null;
    if (props.id) {
      await editExpenseMutation({
        workspaceId: currentWorkspaceId,
        id: props.id,
        title: expense.value.title!,
        datePaid: expense.value.datePaid!,
        currency: expense.value.currency,
        originalAmount: expense.value.originalAmount!,
        convertedAmountInDefaultCurrency: expense.value.convertedAmountInDefaultCurrency ?? null,
        useDifferentExchangeRateForIncomeTaxPurposes: expense.value.useDifferentExchangeRateForIncomeTaxPurposes,
        incomeTaxableAmountInDefaultCurrency: expense.value.incomeTaxableAmountInDefaultCurrency ?? null,
        notes: expense.value.notes ?? null,
        percentOnBusiness: percentOnBusiness ?? null,
        attachments: expense.value.attachments,
        categoryId: expense.value.category ?? null,
        generalTaxId: expense.value.generalTax ?? null,
      });
    } else {
      await createExpenseMutation({
        workspaceId: currentWorkspaceId,
        title: expense.value.title!,
        datePaid: expense.value.datePaid!,
        currency: expense.value.currency,
        originalAmount: expense.value.originalAmount!,
        convertedAmountInDefaultCurrency: expense.value.convertedAmountInDefaultCurrency ?? null,
        useDifferentExchangeRateForIncomeTaxPurposes: expense.value.useDifferentExchangeRateForIncomeTaxPurposes,
        incomeTaxableAmountInDefaultCurrency: expense.value.incomeTaxableAmountInDefaultCurrency ?? null,
        notes: expense.value.notes ?? null,
        percentOnBusiness: percentOnBusiness ?? null,
        attachments: expense.value.attachments,
        categoryId: expense.value.category ?? null,
        generalTaxId: expense.value.generalTax ?? null,
      });
    }
    await navigateToExpensesOverview();
  };

  const {
    formRef,
    submitForm,
    documentsUploadRef,
    onDocumentsUploadComplete,
    onDocumentsUploadFailure,
  } = useFormWithDocumentsUpload(loadExpense, saveExpense);

  const isInForeignCurrency = computed(() => expense.value.currency !== defaultCurrency);

  const pageHeader = props.id
    ? $t.value.editExpense.pageHeader.edit()
    : $t.value.editExpense.pageHeader.create();

</script>
