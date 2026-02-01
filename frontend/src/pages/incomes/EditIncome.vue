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
              v-if="income.useDifferentExchangeRateForIncomeTaxPurposes"
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
                v-model:documents-ids="income.attachments"
                :loading-on-create="id !== undefined"
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
  import type { EditIncomeDto } from '@/services/api';
  import { incomesApi, invoicesApi } from '@/services/api';
  import type { PartialBy } from '@/services/utils';
  import { ensureDefined } from '@/services/utils';
  import { useFormWithDocumentsUpload } from '@/components/form/use-form';

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

  type IncomeFormValues = PartialBy<EditIncomeDto, 'title' | 'originalAmount'> & {
    attachments: Array<number>,
  };

  const income = ref<IncomeFormValues>({
    attachments: [],
    dateReceived: new Date().toISOString().substring(0, 10),
    currency: defaultCurrency,
    useDifferentExchangeRateForIncomeTaxPurposes: false,
    linkedInvoice: props.sourceInvoiceId ? Number(props.sourceInvoiceId) : undefined,
  });

  const loadIncome = async () => {
    if (props.id !== undefined) {
      const loadedIncome = await incomesApi.getIncome({
        incomeId: props.id,
        workspaceId: currentWorkspaceId,
      });
      income.value = {
        ...loadedIncome,
        convertedAmountInDefaultCurrency: loadedIncome.convertedAmounts.originalAmountInDefaultCurrency,
        incomeTaxableAmountInDefaultCurrency: loadedIncome.incomeTaxableAmounts.originalAmountInDefaultCurrency,
      };
    } else if (props.sourceInvoiceId !== undefined) {
      const sourceInvoice = await invoicesApi.getInvoice({
        invoiceId: Number(props.sourceInvoiceId),
        workspaceId: currentWorkspaceId,
      });
      income.value.title = $t.value.editIncome.fromInvoice.title(sourceInvoice.title);
      income.value.currency = sourceInvoice.currency;
      income.value.originalAmount = sourceInvoice.amount;
      income.value.generalTax = sourceInvoice.generalTax;
    }
  };

  const saveIncome = async () => {
    const request: EditIncomeDto = {
      ...(income.value as EditIncomeDto),
    };
    if (props.id) {
      await incomesApi.updateIncome({
        workspaceId: currentWorkspaceId,
        editIncomeDto: request,
        incomeId: ensureDefined(props.id),
      });
    } else {
      await incomesApi.createIncome({
        workspaceId: currentWorkspaceId,
        editIncomeDto: request,
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
