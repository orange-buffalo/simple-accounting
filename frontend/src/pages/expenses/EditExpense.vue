<template>
  <div>
    <div class="sa-page-header">
      <h1>{{ pageHeader }}</h1>
    </div>

    <SaForm
      v-model="formValues"
      :on-submit="saveExpense"
      :on-load="loadExpense"
      :on-cancel="navigateToExpensesOverview"
    >
      <div class="row">
        <div class="col col-xs-12 col-lg-6">
          <h2>{{ $t.editExpense.generalInformation.header() }}</h2>

          <SaFormCategoryInput
            prop="categoryId"
            :label="$t.editExpense.generalInformation.category.label()"
            :placeholder="$t.editExpense.generalInformation.category.placeholder()"
          />

          <SaFormInput
            prop="title"
            :label="$t.editExpense.generalInformation.title.label()"
            :placeholder="$t.editExpense.generalInformation.title.placeholder()"
          />

          <SaFormCurrencyInput
            prop="currency"
            :label="$t.editExpense.generalInformation.currency.label()"
          />

          <SaFormMoneyInput
            prop="originalAmount"
            :label="$t.editExpense.generalInformation.originalAmount.label()"
            :currency="formValues.currency ?? defaultCurrency"
          />

          <SaFormDatePickerInput
            prop="datePaid"
            :label="$t.editExpense.generalInformation.datePaid.label()"
            :placeholder="$t.editExpense.generalInformation.datePaid.placeholder()"
          />

          <SaFormMoneyInput
            v-if="isInForeignCurrency"
            prop="convertedAmountInDefaultCurrency"
            :label="$t.editExpense.generalInformation.convertedAmountInDefaultCurrency.label(defaultCurrency)"
            :currency="defaultCurrency"
          />

          <SaFormCheckbox
            v-if="isInForeignCurrency"
            prop="useDifferentExchangeRateForIncomeTaxPurposes"
            :label="$t.editExpense.generalInformation.useDifferentExchangeRateForIncomeTaxPurposes.label()"
          />

          <!-- eslint-disable-next-line max-len -->
          <SaFormMoneyInput
            v-if="isInForeignCurrency && formValues.useDifferentExchangeRateForIncomeTaxPurposes"
            prop="incomeTaxableAmountInDefaultCurrency"
            :label="$t.editExpense.generalInformation.incomeTaxableAmountInDefaultCurrency.label(defaultCurrency)"
            :currency="defaultCurrency"
          />

          <SaFormGeneralTaxInput
            prop="generalTaxId"
            :label="$t.editExpense.generalInformation.generalTax.label()"
            :placeholder="$t.editExpense.generalInformation.generalTax.placeholder()"
            clearable
          />

          <ElFormItem>
            <ElCheckbox v-model="uiState.partialForBusiness">
              {{ $t.editExpense.generalInformation.partialForBusiness.label() }}
            </ElCheckbox>
          </ElFormItem>

          <SaFormNumberInput
            v-if="uiState.partialForBusiness"
            prop="percentOnBusiness"
            :label="$t.editExpense.generalInformation.percentOnBusiness.label()"
          />
        </div>

        <div class="col col-xs-12 col-lg-6">
          <h2>{{ $t.editExpense.additionalInformation.header() }}</h2>

          <SaFormNotesInput
            prop="notes"
            :label="$t.editExpense.additionalInformation.notes.label()"
            :placeholder="$t.editExpense.additionalInformation.notes.placeholder()"
          />

          <h2>{{ $t.editExpense.attachments.header() }}</h2>

          <SaFormDocumentsUpload
            prop="attachments"
            :documents="resolvedDocuments"
          />
        </div>
      </div>
    </SaForm>
  </div>
</template>

<script lang="ts" setup>
  import { computed, ref } from 'vue';
  import SaForm from '@/components/form/SaForm.vue';
  import SaFormInput from '@/components/form/SaFormInput.vue';
  import SaFormMoneyInput from '@/components/form/SaFormMoneyInput.vue';
  import SaFormCurrencyInput from '@/components/form/SaFormCurrencyInput.vue';
  import SaFormDatePickerInput from '@/components/form/SaFormDatePickerInput.vue';
  import SaFormCheckbox from '@/components/form/SaFormCheckbox.vue';
  import SaFormNotesInput from '@/components/form/SaFormNotesInput.vue';
  import SaFormDocumentsUpload from '@/components/form/SaFormDocumentsUpload.vue';
  import SaFormNumberInput from '@/components/form/SaFormNumberInput.vue';
  import SaFormCategoryInput from '@/components/form/SaFormCategoryInput.vue';
  import SaFormGeneralTaxInput from '@/components/form/SaFormGeneralTaxInput.vue';
  import { $t } from '@/services/i18n';
  import useNavigation from '@/services/use-navigation';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import { formatDateToLocalISOString } from '@/services/date-utils';
  import { graphql } from '@/services/api/gql';
  import { useMutation, useLazyQuery } from '@/services/api/use-gql-api.ts';
  import { useDocumentAttachments } from '@/components/documents/documents-gql-types';
  import {
    CreateExpenseMutationMutationVariables,
    EditExpenseMutationMutationVariables,
  } from '@/services/api/gql/graphql.ts';
  import { AsFormValues } from '@/components/form/sa-form-api.ts';

  const props = defineProps<{
    id?: number,
    prototype?: string,
  }>();

  const { navigateByViewName } = useNavigation();
  const navigateToExpensesOverview = async () => {
    await navigateByViewName('expenses-overview');
  };

  const {
    currentWorkspaceId,
    defaultCurrency,
  } = useCurrentWorkspace();

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
            ...DocumentData
          }
        }
      }
    }
  `), 'workspace');

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

  type ExpenseFormValues = AsFormValues<[
    CreateExpenseMutationMutationVariables,
    EditExpenseMutationMutationVariables,
  ]>;

  const formValues = ref<ExpenseFormValues>({
    workspaceId: currentWorkspaceId,
    id: props.id,
    datePaid: formatDateToLocalISOString(new Date()),
    currency: defaultCurrency,
    useDifferentExchangeRateForIncomeTaxPurposes: false,
    percentOnBusiness: 100,
    attachments: [],
  });

  const { resolvedDocuments, setDocuments } = useDocumentAttachments();

  const uiState = ref<{
    partialForBusiness: boolean,
  }>({
    partialForBusiness: false,
  });

  const loadExpense = (props.id !== undefined || props.prototype !== undefined) ? async () => {
    const isEdit = props.id !== undefined;
    const expenseId = isEdit ? props.id! : Number(props.prototype);
    const workspace = await getExpenseQuery({
      workspaceId: currentWorkspaceId,
      expenseId,
    });
    const loaded = workspace?.expense;
    if (loaded) {
      formValues.value = {
        ...formValues.value,
        ...loaded,
        categoryId: loaded.category?.id ?? null,
        notes: loaded.notes ?? null,
        generalTaxId: loaded.generalTaxId ?? null,
        attachments: isEdit ? setDocuments(loaded.attachments) : [],
        ...(isEdit ? {
          datePaid: loaded.datePaid,
          convertedAmountInDefaultCurrency: loaded.convertedAmounts.originalAmountInDefaultCurrency ?? null,
          incomeTaxableAmountInDefaultCurrency: loaded.incomeTaxableAmounts.originalAmountInDefaultCurrency ?? null,
        } : {
          datePaid: null,
          convertedAmountInDefaultCurrency: null,
          incomeTaxableAmountInDefaultCurrency: null,
        }),
      };
      uiState.value.partialForBusiness = loaded.percentOnBusiness !== 100;
    }
  } : undefined;

  const saveExpense = async () => {
    const effectivePercentOnBusiness = uiState.value.partialForBusiness
      ? formValues.value.percentOnBusiness ?? null
      : null;
    const requestArgs = {
      ...formValues.value,
      percentOnBusiness: effectivePercentOnBusiness,
    };
    if (props.id) {
      await editExpenseMutation(requestArgs as EditExpenseMutationMutationVariables);
    } else {
      await createExpenseMutation(requestArgs as CreateExpenseMutationMutationVariables);
    }
    await navigateToExpensesOverview();
  };

  const isInForeignCurrency = computed(() => formValues.value.currency !== defaultCurrency);

  const pageHeader = computed(() => props.id !== undefined
    ? $t.value.editExpense.pageHeader.edit()
    : $t.value.editExpense.pageHeader.create());
</script>
