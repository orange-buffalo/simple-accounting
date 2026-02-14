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
import SaCategoryInput from '@/components/category/SaCategoryInput.vue';
import SaCurrencyInput from '@/components/currency-input/SaCurrencyInput.vue';
import SaDocumentsUpload from '@/components/documents/SaDocumentsUpload.vue';
import SaLegacyForm from '@/components/form/SaLegacyForm.vue';
import { useFormWithDocumentsUpload } from '@/components/form/use-form';
import SaGeneralTaxInput from '@/components/general-tax/SaGeneralTaxInput.vue';
import SaNotesInput from '@/components/notes-input/SaNotesInput.vue';
import SaMoneyInput from '@/components/SaMoneyInput.vue';
import type { EditExpenseDto } from '@/services/api';
import { expensesApi } from '@/services/api';
import { formatDateToLocalISOString } from '@/services/date-utils';
import { $t } from '@/services/i18n';
import useNavigation from '@/services/use-navigation';
import type { PartialBy } from '@/services/utils';
import { ensureDefined } from '@/services/utils';
import { useCurrentWorkspace } from '@/services/workspaces';

const props = defineProps<{
  id?: number;
  prototype?: string;
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

const { currentWorkspaceId, defaultCurrency } = useCurrentWorkspace();

type ExpenseFormValues = PartialBy<EditExpenseDto, 'datePaid' | 'title' | 'originalAmount'> & {
  attachments: Array<number>;
};

const expense = ref<ExpenseFormValues>({
  attachments: [],
  percentOnBusiness: 100,
  datePaid: formatDateToLocalISOString(new Date()),
  currency: defaultCurrency,
  useDifferentExchangeRateForIncomeTaxPurposes: false,
});

const uiState = ref<{
  partialForBusiness: boolean;
}>({
  partialForBusiness: false,
});

const loadExpense = async () => {
  if (props.id !== undefined) {
    const fullExpense = await expensesApi.getExpense({
      expenseId: props.id,
      workspaceId: currentWorkspaceId,
    });
    expense.value = {
      ...fullExpense,
      convertedAmountInDefaultCurrency: fullExpense.convertedAmounts.originalAmountInDefaultCurrency,
      incomeTaxableAmountInDefaultCurrency: fullExpense.incomeTaxableAmounts.originalAmountInDefaultCurrency,
    };
    uiState.value.partialForBusiness = fullExpense.percentOnBusiness !== 100;
  } else if (props.prototype !== undefined) {
    const prototypeExpense = await expensesApi.getExpense({
      expenseId: Number(props.prototype),
      workspaceId: currentWorkspaceId,
    });
    expense.value = prototypeExpense;
    expense.value.datePaid = undefined;
    uiState.value.partialForBusiness = prototypeExpense.percentOnBusiness !== 100;
  }
};

const saveExpense = async () => {
  const request: EditExpenseDto = {
    ...(expense.value as EditExpenseDto),
    percentOnBusiness: uiState.value.partialForBusiness ? expense.value.percentOnBusiness : undefined,
  };
  if (props.id) {
    await expensesApi.updateExpense({
      workspaceId: currentWorkspaceId,
      editExpenseDto: request,
      expenseId: ensureDefined(props.id),
    });
  } else {
    await expensesApi.createExpense({
      workspaceId: currentWorkspaceId,
      editExpenseDto: request,
    });
  }
  await navigateToExpensesOverview();
};

const { formRef, submitForm, documentsUploadRef, onDocumentsUploadComplete, onDocumentsUploadFailure } =
  useFormWithDocumentsUpload(loadExpense, saveExpense);

const isInForeignCurrency = computed(() => expense.value.currency !== defaultCurrency);

const pageHeader = props.id ? $t.value.editExpense.pageHeader.edit() : $t.value.editExpense.pageHeader.create();
</script>
