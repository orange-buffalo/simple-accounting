<template>
  <div>
    <div class="sa-page-header">
      <h1>{{ pageHeader }}</h1>
    </div>

    <SaForm
      v-model="formValues"
      :on-submit="saveIncome"
      :on-load="loadIncome"
      :on-cancel="navigateToIncomesOverview"
    >
      <div class="row">
        <div class="col col-xs-12 col-lg-6">
          <h2>{{ $t.editIncome.generalInformation.header() }}</h2>

          <SaFormCategoryInput
            prop="categoryId"
            :label="$t.editIncome.generalInformation.category.label()"
            :placeholder="$t.editIncome.generalInformation.category.placeholder()"
          />

          <SaFormInput
            prop="title"
            :label="$t.editIncome.generalInformation.title.label()"
            :placeholder="$t.editIncome.generalInformation.title.placeholder()"
          />

          <SaFormCurrencyInput
            prop="currency"
            :label="$t.editIncome.generalInformation.currency.label()"
          />

          <SaFormMoneyInput
            prop="originalAmount"
            :label="$t.editIncome.generalInformation.originalAmount.label()"
            :currency="formValues.currency ?? defaultCurrency"
          />

          <SaFormDatePickerInput
            prop="dateReceived"
            :label="$t.editIncome.generalInformation.dateReceived.label()"
            :placeholder="$t.editIncome.generalInformation.dateReceived.placeholder()"
          />

          <SaFormMoneyInput
            v-if="isInForeignCurrency"
            prop="convertedAmountInDefaultCurrency"
            :label="$t.editIncome.generalInformation.convertedAmountInDefaultCurrency.label(defaultCurrency)"
            :currency="defaultCurrency"
          />

          <SaFormCheckbox
            v-if="isInForeignCurrency"
            prop="useDifferentExchangeRateForIncomeTaxPurposes"
            :label="$t.editIncome.generalInformation.useDifferentExchangeRateForIncomeTaxPurposes.label()"
          />

          <!-- eslint-disable-next-line max-len -->
          <SaFormMoneyInput
            v-if="isInForeignCurrency && formValues.useDifferentExchangeRateForIncomeTaxPurposes"
            prop="incomeTaxableAmountInDefaultCurrency"
            :label="$t.editIncome.generalInformation.incomeTaxableAmountInDefaultCurrency.label(defaultCurrency)"
            :currency="defaultCurrency"
          />

          <SaFormGeneralTaxInput
            prop="generalTaxId"
            :label="$t.editIncome.generalInformation.generalTax.label()"
            :placeholder="$t.editIncome.generalInformation.generalTax.placeholder()"
            clearable
          />
        </div>

        <div class="col col-xs-12 col-lg-6">
          <h2>{{ $t.editIncome.additionalInformation.header() }}</h2>

          <SaFormInvoiceSelect
            prop="linkedInvoiceId"
            :label="$t.editIncome.additionalInformation.linkedInvoice.label()"
          />

          <SaFormNotesInput
            prop="notes"
            :label="$t.editIncome.additionalInformation.notes.label()"
            :placeholder="$t.editIncome.additionalInformation.notes.placeholder()"
          />

          <h2>{{ $t.editIncome.attachments.header() }}</h2>

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
  import SaFormCategoryInput from '@/components/form/SaFormCategoryInput.vue';
  import SaFormGeneralTaxInput from '@/components/form/SaFormGeneralTaxInput.vue';
  import SaFormInvoiceSelect from '@/components/form/SaFormInvoiceSelect.vue';
  import useNavigation from '@/services/use-navigation';
  import { $t } from '@/services/i18n';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import { formatDateToLocalISOString } from '@/services/date-utils';
  import { graphql } from '@/services/api/gql';
  import { useMutation, useLazyQuery } from '@/services/api/use-gql-api.ts';
  import { useDocumentAttachments } from '@/components/documents/documents-gql-types';
  import {
    CreateIncomeMutationMutationVariables,
    EditIncomeMutationMutationVariables,
  } from '@/services/api/gql/graphql.ts';
  import { AsFormValues, toRequestArgs, updateFormValues } from '@/components/form/sa-form-api.ts';

  const props = defineProps<{
    id?: number,
    sourceInvoiceId?: string,
  }>();

  const { navigateByViewName } = useNavigation();
  const navigateToIncomesOverview = async () => {
    await navigateByViewName('incomes-overview');
  };

  const {
    defaultCurrency,
    currentWorkspaceId,
  } = useCurrentWorkspace();

  const getSourceInvoiceQuery = useLazyQuery(graphql(`
    query getSourceInvoiceForIncome($workspaceId: Long!, $invoiceId: Long!) {
      workspace(id: $workspaceId) {
        invoice(id: $invoiceId) {
          title
          currency
          amount
          generalTax {
            id
          }
        }
      }
    }
  `), 'workspace');

  const getIncomeForEditQuery = useLazyQuery(graphql(`
    query getIncomeForEdit($workspaceId: Long!, $incomeId: Long!) {
      workspace(id: $workspaceId) {
        income(id: $incomeId) {
          id
          category {
            id
          }
          title
          dateReceived
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
          generalTax {
            id
          }
          linkedInvoice {
            id
          }
          attachments {
            ...DocumentData
          }
        }
      }
    }
  `), 'workspace');

  const createIncomeMutation = useMutation(graphql(`
    mutation createIncomeMutation(
      $workspaceId: Long!,
      $title: String!,
      $dateReceived: LocalDate!,
      $currency: String!,
      $originalAmount: Long!,
      $convertedAmountInDefaultCurrency: Long,
      $useDifferentExchangeRateForIncomeTaxPurposes: Boolean!,
      $incomeTaxableAmountInDefaultCurrency: Long,
      $notes: String,
      $attachments: [Long!],
      $categoryId: Long,
      $generalTaxId: Long,
      $linkedInvoiceId: Long
    ) {
      createIncome(
        workspaceId: $workspaceId,
        title: $title,
        dateReceived: $dateReceived,
        currency: $currency,
        originalAmount: $originalAmount,
        convertedAmountInDefaultCurrency: $convertedAmountInDefaultCurrency,
        useDifferentExchangeRateForIncomeTaxPurposes: $useDifferentExchangeRateForIncomeTaxPurposes,
        incomeTaxableAmountInDefaultCurrency: $incomeTaxableAmountInDefaultCurrency,
        notes: $notes,
        attachments: $attachments,
        categoryId: $categoryId,
        generalTaxId: $generalTaxId,
        linkedInvoiceId: $linkedInvoiceId
      ) {
        id
      }
    }
  `), 'createIncome');

  const editIncomeMutation = useMutation(graphql(`
    mutation editIncomeMutation(
      $workspaceId: Long!,
      $id: Long!,
      $title: String!,
      $dateReceived: LocalDate!,
      $currency: String!,
      $originalAmount: Long!,
      $convertedAmountInDefaultCurrency: Long,
      $useDifferentExchangeRateForIncomeTaxPurposes: Boolean!,
      $incomeTaxableAmountInDefaultCurrency: Long,
      $notes: String,
      $attachments: [Long!],
      $categoryId: Long,
      $generalTaxId: Long,
      $linkedInvoiceId: Long
    ) {
      editIncome(
        workspaceId: $workspaceId,
        id: $id,
        title: $title,
        dateReceived: $dateReceived,
        currency: $currency,
        originalAmount: $originalAmount,
        convertedAmountInDefaultCurrency: $convertedAmountInDefaultCurrency,
        useDifferentExchangeRateForIncomeTaxPurposes: $useDifferentExchangeRateForIncomeTaxPurposes,
        incomeTaxableAmountInDefaultCurrency: $incomeTaxableAmountInDefaultCurrency,
        notes: $notes,
        attachments: $attachments,
        categoryId: $categoryId,
        generalTaxId: $generalTaxId,
        linkedInvoiceId: $linkedInvoiceId
      ) {
        id
      }
    }
  `), 'editIncome');

  type IncomeFormValues = AsFormValues<[
    CreateIncomeMutationMutationVariables,
    EditIncomeMutationMutationVariables,
  ]>;

  const formValues = ref<IncomeFormValues>({
    workspaceId: currentWorkspaceId,
    id: props.id,
    dateReceived: formatDateToLocalISOString(new Date()),
    currency: defaultCurrency,
    useDifferentExchangeRateForIncomeTaxPurposes: false,
    linkedInvoiceId: props.sourceInvoiceId ? Number(props.sourceInvoiceId) : undefined,
    attachments: [],
  });

  const { resolvedDocuments, setDocuments } = useDocumentAttachments();

  const loadIncome = (props.id !== undefined || props.sourceInvoiceId !== undefined) ? async () => {
    if (props.id !== undefined) {
      const workspace = await getIncomeForEditQuery({
        workspaceId: currentWorkspaceId,
        incomeId: props.id,
      });
      updateFormValues(formValues, workspace.income, income => ({
        categoryId: income.category?.id ?? null,
        convertedAmountInDefaultCurrency: income.convertedAmounts.originalAmountInDefaultCurrency ?? null,
        incomeTaxableAmountInDefaultCurrency:
          income.incomeTaxableAmounts.originalAmountInDefaultCurrency ?? null,
        notes: income.notes ?? null,
        generalTaxId: income.generalTax?.id ?? null,
        linkedInvoiceId: income.linkedInvoice?.id ?? null,
        attachments: setDocuments(income.attachments),
      }));
    } else {
      const workspace = await getSourceInvoiceQuery({
        workspaceId: currentWorkspaceId,
        invoiceId: Number(props.sourceInvoiceId),
      });
      const sourceInvoice = workspace.invoice;
      if (sourceInvoice) {
        formValues.value = {
          ...formValues.value,
          title: $t.value.editIncome.fromInvoice.title(sourceInvoice.title),
          currency: sourceInvoice.currency,
          originalAmount: sourceInvoice.amount,
          generalTaxId: sourceInvoice.generalTax?.id ?? null,
        };
      }
    }
  } : undefined;

  const saveIncome = async () => {
    if (props.id) {
      await editIncomeMutation(toRequestArgs(formValues));
    } else {
      await createIncomeMutation(toRequestArgs(formValues));
    }
    await navigateToIncomesOverview();
  };

  const isInForeignCurrency = computed(() => formValues.value.currency !== defaultCurrency);

  const pageHeader = computed(() => props.id
    ? $t.value.editIncome.pageHeader.edit()
    : $t.value.editIncome.pageHeader.create());
</script>
