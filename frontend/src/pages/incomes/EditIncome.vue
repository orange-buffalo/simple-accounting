<template>
  <div>
    <div class="sa-page-header">
      <h1>{{ pageHeader }}</h1>
    </div>

    <SaLegacyForm
      ref="formRef"
      :model="income"
      :rules="incomeValidationRules"
    >
      <template #default>
        <div class="row">
          <div class="col col-xs-12 col-lg-6">
            <h2>{{ $t.editIncome.generalInformation.header() }}</h2>

            <ElFormItem
              :label="$t.editIncome.generalInformation.category.label()"
              prop="category"
            >
              <SaCategoryInput
                v-model="income.category"
                :placeholder="$t.editIncome.generalInformation.category.placeholder()"
              />
            </ElFormItem>

            <ElFormItem
              :label="$t.editIncome.generalInformation.title.label()"
              prop="title"
            >
              <ElInput
                v-model="income.title"
                :placeholder="$t.editIncome.generalInformation.title.placeholder()"
              />
            </ElFormItem>

            <ElFormItem
              :label="$t.editIncome.generalInformation.currency.label()"
              prop="currency"
            >
              <SaCurrencyInput v-model="income.currency" />
            </ElFormItem>

            <ElFormItem
              :label="$t.editIncome.generalInformation.originalAmount.label()"
              prop="originalAmount"
            >
              <SaMoneyInput
                v-model="income.originalAmount"
                :currency="income.currency"
              />
            </ElFormItem>

            <ElFormItem
              :label="$t.editIncome.generalInformation.dateReceived.label()"
              prop="dateReceived"
            >
              <!-- todo #78: format from cldr https://github.com/ElemeFE/element/issues/11353 -->
              <ElDatePicker
                v-model="income.dateReceived"
                type="date"
                value-format="YYYY-MM-DD"
                :placeholder="$t.editIncome.generalInformation.dateReceived.placeholder()"
              />
            </ElFormItem>

            <ElFormItem
              v-if="isInForeignCurrency"
              :label="$t.editIncome.generalInformation.convertedAmountInDefaultCurrency.label(defaultCurrency)"
              prop="convertedAmountInDefaultCurrency"
            >
              <SaMoneyInput
                v-model="income.convertedAmountInDefaultCurrency"
                :currency="defaultCurrency"
              />
            </ElFormItem>

            <ElFormItem v-if="isInForeignCurrency">
              <ElCheckbox v-model="income.useDifferentExchangeRateForIncomeTaxPurposes">
                {{ $t.editIncome.generalInformation.useDifferentExchangeRateForIncomeTaxPurposes.label() }}
              </ElCheckbox>
            </ElFormItem>

            <ElFormItem
              v-if="isInForeignCurrency && income.useDifferentExchangeRateForIncomeTaxPurposes"
              :label="$t.editIncome.generalInformation.incomeTaxableAmountInDefaultCurrency.label(defaultCurrency)"
              prop="incomeTaxableAmountInDefaultCurrency"
            >
              <SaMoneyInput
                v-model="income.incomeTaxableAmountInDefaultCurrency"
                :currency="defaultCurrency"
              />
            </ElFormItem>

            <ElFormItem
              :label="$t.editIncome.generalInformation.generalTax.label()"
              prop="generalTax"
            >
              <SaGeneralTaxInput
                v-model="income.generalTax"
                clearable
                :placeholder="$t.editIncome.generalInformation.generalTax.placeholder()"
              />
            </ElFormItem>
          </div>

          <div class="col col-xs-12 col-lg-6">
            <h2>{{ $t.editIncome.additionalInformation.header() }}</h2>

            <ElFormItem
              :label="$t.editIncome.additionalInformation.linkedInvoice.label()"
              prop="reportedAmountInDefaultCurrency"
            >
              <SaInvoiceSelect v-model="income.linkedInvoice" />
            </ElFormItem>

            <ElFormItem
              :label="$t.editIncome.additionalInformation.notes.label()"
              prop="notes"
            >
              <SaNotesInput
                v-model="income.notes"
                :placeholder="$t.editIncome.additionalInformation.notes.placeholder()"
              />
            </ElFormItem>

            <h2>{{ $t.editIncome.attachments.header() }}</h2>

            <ElFormItem>
              <SaDocumentsUpload
                ref="documentsUploadRef"
                :documents="resolvedDocuments"
                :loading-on-create="id !== undefined"
                @update:documents-ids="income.attachments = $event"
                @uploads-completed="onDocumentsUploadComplete"
                @uploads-failed="onDocumentsUploadFailure"
              />
            </ElFormItem>
          </div>
        </div>
      </template>

      <template #buttons-bar>
        <ElButton @click="navigateToIncomesOverview">
          {{ $t.editIncome.cancel() }}
        </ElButton>
        <ElButton
          type="primary"
          @click="submitForm"
        >
          {{ $t.editIncome.save() }}
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
  import useNavigation from '@/services/use-navigation';
  import { $t } from '@/services/i18n';
  import SaInvoiceSelect from '@/components/entity-select/SaInvoiceSelect.vue';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import { useFormWithDocumentsUpload } from '@/components/form/use-form';
  import { formatDateToLocalISOString } from '@/services/date-utils';
  import { graphql } from '@/services/api/gql';
  import { useMutation, useLazyQuery } from '@/services/api/use-gql-api.ts';
  import { useFragment } from '@/services/api/gql/fragment-masking';
  import {
    DocumentDataFragment,
    type DocumentDataFragmentType,
  } from '@/components/documents/documents-gql-types';

  const props = defineProps<{
    id?: number,
    sourceInvoiceId?: string,
  }>();

  const { navigateByViewName } = useNavigation();
  const navigateToIncomesOverview = async () => {
    await navigateByViewName('incomes-overview');
  };

  const incomeValidationRules = {
    currency: {
      required: true,
      message: $t.value.editIncome.validations.currency(),
    },
    title: {
      required: true,
      message: $t.value.editIncome.validations.title(),
    },
    dateReceived: {
      required: true,
      message: $t.value.editIncome.validations.dateReceived(),
    },
    originalAmount: {
      required: true,
      message: $t.value.editIncome.validations.originalAmount(),
    },
  };

  const {
    defaultCurrency,
    currentWorkspaceId,
  } = useCurrentWorkspace();

  type IncomeFormValues = {
    category?: number,
    title?: string,
    dateReceived?: string,
    currency: string,
    originalAmount?: number,
    convertedAmountInDefaultCurrency?: number,
    useDifferentExchangeRateForIncomeTaxPurposes: boolean,
    incomeTaxableAmountInDefaultCurrency?: number,
    notes?: string,
    generalTax?: number,
    attachments: number[],
    linkedInvoice?: number,
  };

  const income = ref<IncomeFormValues>({
    attachments: [],
    dateReceived: formatDateToLocalISOString(new Date()),
    currency: defaultCurrency,
    useDifferentExchangeRateForIncomeTaxPurposes: false,
    linkedInvoice: props.sourceInvoiceId ? Number(props.sourceInvoiceId) : undefined,
  });

  const resolvedDocuments = ref<DocumentDataFragmentType[]>([]);

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

  const loadIncome = async () => {
    if (props.id !== undefined) {
      const workspace = await getIncomeForEditQuery({
        workspaceId: currentWorkspaceId,
        incomeId: props.id,
      });
      const loaded = workspace?.income;
      if (loaded) {
        resolvedDocuments.value = [...loaded.attachments];
        income.value = {
          category: loaded.category?.id ?? undefined,
          title: loaded.title,
          dateReceived: loaded.dateReceived,
          currency: loaded.currency,
          originalAmount: loaded.originalAmount,
          convertedAmountInDefaultCurrency: loaded.convertedAmounts.originalAmountInDefaultCurrency ?? undefined,
          useDifferentExchangeRateForIncomeTaxPurposes: loaded.useDifferentExchangeRateForIncomeTaxPurposes,
          incomeTaxableAmountInDefaultCurrency:
            loaded.incomeTaxableAmounts.originalAmountInDefaultCurrency ?? undefined,
          notes: loaded.notes ?? undefined,
          generalTax: loaded.generalTax?.id ?? undefined,
          linkedInvoice: loaded.linkedInvoice?.id ?? undefined,
          attachments: loaded.attachments.map(a => useFragment(DocumentDataFragment, a).id),
        };
      }
    } else if (props.sourceInvoiceId !== undefined) {
      const workspace = await getSourceInvoiceQuery({
        workspaceId: currentWorkspaceId,
        invoiceId: Number(props.sourceInvoiceId),
      });
      const sourceInvoice = workspace?.invoice;
      if (sourceInvoice) {
        income.value.title = $t.value.editIncome.fromInvoice.title(sourceInvoice.title);
        income.value.currency = sourceInvoice.currency;
        income.value.originalAmount = sourceInvoice.amount;
        income.value.generalTax = sourceInvoice.generalTax?.id ?? undefined;
      }
    }
  };

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

  const saveIncome = async () => {
    if (props.id) {
      await editIncomeMutation({
        workspaceId: currentWorkspaceId,
        id: props.id,
        title: income.value.title!,
        dateReceived: income.value.dateReceived!,
        currency: income.value.currency,
        originalAmount: income.value.originalAmount!,
        convertedAmountInDefaultCurrency: income.value.convertedAmountInDefaultCurrency ?? null,
        useDifferentExchangeRateForIncomeTaxPurposes: income.value.useDifferentExchangeRateForIncomeTaxPurposes,
        incomeTaxableAmountInDefaultCurrency: income.value.incomeTaxableAmountInDefaultCurrency ?? null,
        notes: income.value.notes ?? null,
        attachments: income.value.attachments,
        categoryId: income.value.category ?? null,
        generalTaxId: income.value.generalTax ?? null,
        linkedInvoiceId: income.value.linkedInvoice ?? null,
      });
    } else {
      await createIncomeMutation({
        workspaceId: currentWorkspaceId,
        title: income.value.title!,
        dateReceived: income.value.dateReceived!,
        currency: income.value.currency,
        originalAmount: income.value.originalAmount!,
        convertedAmountInDefaultCurrency: income.value.convertedAmountInDefaultCurrency ?? null,
        useDifferentExchangeRateForIncomeTaxPurposes: income.value.useDifferentExchangeRateForIncomeTaxPurposes,
        incomeTaxableAmountInDefaultCurrency: income.value.incomeTaxableAmountInDefaultCurrency ?? null,
        notes: income.value.notes ?? null,
        attachments: income.value.attachments,
        categoryId: income.value.category ?? null,
        generalTaxId: income.value.generalTax ?? null,
        linkedInvoiceId: income.value.linkedInvoice ?? null,
      });
    }
    await navigateToIncomesOverview();
  };

  const {
    formRef,
    submitForm,
    documentsUploadRef,
    onDocumentsUploadComplete,
    onDocumentsUploadFailure,
  } = useFormWithDocumentsUpload(loadIncome, saveIncome);

  const isInForeignCurrency = computed(() => income.value.currency !== defaultCurrency);

  const pageHeader = props.id
    ? $t.value.editIncome.pageHeader.edit()
    : $t.value.editIncome.pageHeader.create();
</script>
