<template>
  <div>
    <div class="sa-page-header">
      <h1>{{ pageHeader }}</h1>

      <div
        v-if="uiState.isEditing"
        class="sa-header-options"
      >
        <span />
        <ElButton
          v-if="uiState.status !== 'CANCELLED'"
          type="danger"
          @click="cancelInvoice"
        >
          {{ $t.editInvoice.cancelInvoice.button() }}
        </ElButton>
        <SaStatusLabel
          v-if="uiState.status === 'CANCELLED'"
          status="failure"
        >
          {{ $t.editInvoice.cancelInvoice.status() }}
        </SaStatusLabel>
      </div>
    </div>

    <SaLegacyForm
      ref="formRef"
      :model="invoice"
      :rules="invoiceValidationRules"
      :initially-loading="id !== undefined"
    >
      <template #default>
        <div class="row">
          <div class="col col-xs-12 col-lg-6">
            <h2>{{ $t.editInvoice.generalInformation.header() }}</h2>

            <ElFormItem
              :label="$t.editInvoice.generalInformation.customer.label()"
              prop="customer"
            >
              <SaCustomerInput
                v-model="invoice.customer"
                :placeholder="$t.editInvoice.generalInformation.customer.placeholder()"
              />
            </ElFormItem>

            <ElFormItem
              :label="$t.editInvoice.generalInformation.title.label()"
              prop="title"
            >
              <ElInput
                v-model="invoice.title"
                :placeholder="$t.editInvoice.generalInformation.title.placeholder()"
              />
            </ElFormItem>

            <ElFormItem
              :label="$t.editInvoice.generalInformation.currency.label()"
              prop="currency"
            >
              <SaCurrencyInput v-model="invoice.currency" />
            </ElFormItem>

            <ElFormItem
              :label="$t.editInvoice.generalInformation.amount.label()"
              prop="amount"
            >
              <SaMoneyInput
                v-model="invoice.amount"
                :currency="invoice.currency"
              />
            </ElFormItem>

            <ElFormItem
              :label="$t.editInvoice.generalInformation.dateIssued.label()"
              prop="dateIssued"
            >
              <!-- todo #78: format from cldr https://github.com/ElemeFE/element/issues/11353 -->
              <ElDatePicker
                v-model="invoice.dateIssued"
                type="date"
                value-format="YYYY-MM-DD"
                :placeholder="$t.editInvoice.generalInformation.dateIssued.placeholder()"
              />
            </ElFormItem>

            <ElFormItem
              :label="$t.editInvoice.generalInformation.dueDate.label()"
              prop="dueDate"
            >
              <!-- todo #78: format from cldr https://github.com/ElemeFE/element/issues/11353 -->
              <ElDatePicker
                v-model="invoice.dueDate"
                type="date"
                value-format="YYYY-MM-DD"
                :placeholder="$t.editInvoice.generalInformation.dueDate.placeholder()"
              />
            </ElFormItem>

            <ElFormItem
              :label="$t.editInvoice.generalInformation.generalTax.label()"
              prop="generalTax"
            >
              <SaGeneralTaxInput
                v-model="invoice.generalTax"
                clearable
                :placeholder="$t.editInvoice.generalInformation.generalTax.placeholder()"
              />
            </ElFormItem>

            <ElFormItem>
              <ElCheckbox v-model="uiState.alreadySent">
                {{ $t.editInvoice.generalInformation.alreadySent.label() }}
              </ElCheckbox>
            </ElFormItem>

            <ElFormItem
              v-if="uiState.alreadySent"
              :label="$t.editInvoice.generalInformation.dateSent.label()"
              prop="dateSent"
            >
              <!-- todo #78: format from cldr https://github.com/ElemeFE/element/issues/11353 -->
              <ElDatePicker
                v-model="invoice.dateSent"
                type="date"
                value-format="YYYY-MM-DD"
                :placeholder="$t.editInvoice.generalInformation.dateSent.placeholder()"
              />
            </ElFormItem>

            <ElFormItem>
              <ElCheckbox v-model="uiState.alreadyPaid">
                {{ $t.editInvoice.generalInformation.alreadyPaid.label() }}
              </ElCheckbox>
            </ElFormItem>

            <ElFormItem
              v-if="uiState.alreadyPaid"
              :label="$t.editInvoice.generalInformation.datePaid.label()"
              prop="datePaid"
            >
              <!-- todo #78: format from cldr https://github.com/ElemeFE/element/issues/11353 -->
              <ElDatePicker
                v-model="invoice.datePaid"
                type="date"
                value-format="YYYY-MM-DD"
                :placeholder="$t.editInvoice.generalInformation.datePaid.placeholder()"
              />
            </ElFormItem>
          </div>

          <div class="col col-xs-12 col-lg-6">
            <h2>{{ $t.editInvoice.additionalNotes.header() }}</h2>

            <ElFormItem
              :label="$t.editInvoice.additionalNotes.notes.label()"
              prop="notes"
            >
              <SaNotesInput
                v-model="invoice.notes"
                :placeholder="$t.editInvoice.additionalNotes.notes.placeholder()"
              />
            </ElFormItem>

            <h2>{{ $t.editInvoice.attachments.header() }}</h2>

            <ElFormItem>
              <SaDocumentsUpload
                ref="documentsUploadRef"
                v-model:documents-ids="invoice.attachments"
                :loading-on-create="id !== undefined"
                @uploads-completed="onDocumentsUploadComplete"
                @uploads-failed="onDocumentsUploadFailure"
              />
            </ElFormItem>
          </div>
        </div>
      </template>

      <template #buttons-bar>
        <ElButton @click="navigateToInvoicesOverview">
          {{ $t.editInvoice.cancel() }}
        </ElButton>
        <ElButton
          type="primary"
          @click="submitForm"
        >
          {{ $t.editInvoice.save() }}
        </ElButton>
      </template>
    </SaLegacyForm>
  </div>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import { $t } from '@/services/i18n';
  import SaMoneyInput from '@/components/SaMoneyInput.vue';
  import SaCurrencyInput from '@/components/currency-input/SaCurrencyInput.vue';
  import SaDocumentsUpload from '@/components/documents/SaDocumentsUpload.vue';
  import SaNotesInput from '@/components/notes-input/SaNotesInput.vue';
  import SaLegacyForm from '@/components/form/SaLegacyForm.vue';
  import SaCustomerInput from '@/components/customer/SaCustomerInput.vue';
  import SaGeneralTaxInput from '@/components/general-tax/SaGeneralTaxInput.vue';
  import useNavigation from '@/services/use-navigation';
  import { useFormWithDocumentsUpload } from '@/components/form/use-form';
  import SaStatusLabel from '@/components/SaStatusLabel.vue';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import type { EditInvoiceDto, InvoiceDtoStatusEnum } from '@/services/api';
  import { invoicesApi } from '@/services/api';
  import { useConfirmation } from '@/components/confirmation/use-confirmation';
  import type { PartialBy } from '@/services/utils';
  import { ensureDefined } from '@/services/utils';

  const props = defineProps<{
    id?: number
  }>();

  const invoiceValidationRules = {
    customer: {
      required: true,
      message: $t.value.editInvoice.validations.customer(),
    },
    currency: {
      required: true,
      message: $t.value.editInvoice.validations.currency(),
    },
    title: {
      required: true,
      message: $t.value.editInvoice.validations.title(),
    },
    amount: {
      required: true,
      message: $t.value.editInvoice.validations.amount(),
    },
    dateIssued: {
      required: true,
      message: $t.value.editInvoice.validations.dateIssued(),
    },
    dueDate: {
      required: true,
      message: $t.value.editInvoice.validations.dueDate(),
    },
    dateSent: {
      required: true,
      message: $t.value.editInvoice.validations.dateSent(),
    },
    datePaid: {
      required: true,
      message: $t.value.editInvoice.validations.datePaid(),
    },
  };

  const { navigateByViewName } = useNavigation();
  const navigateToInvoicesOverview = async () => {
    await navigateByViewName('invoices-overview');
  };

  const {
    currentWorkspaceId,
    defaultCurrency,
  } = useCurrentWorkspace();

  type InvoiceFormValues = PartialBy<EditInvoiceDto, 'amount' | 'dueDate' | 'customer' | 'title'> & {
    attachments: Array<number>,
  };

  const invoice = ref<InvoiceFormValues>({
    attachments: [],
    dateIssued: new Date().toISOString().substring(0, 10),
    currency: defaultCurrency,
  });

  const uiState = ref<{
    alreadySent: boolean,
    alreadyPaid: boolean,
    isEditing: boolean,
    status?: InvoiceDtoStatusEnum
  }>({
    alreadySent: false,
    alreadyPaid: false,
    isEditing: props.id != null,
  });

  const loadInvoice = async () => {
    if (props.id !== undefined) {
      const fullInvoice = await invoicesApi.getInvoice({
        invoiceId: props.id,
        workspaceId: currentWorkspaceId,
      });
      invoice.value = fullInvoice;
      uiState.value.alreadyPaid = invoice.value.datePaid !== undefined;
      uiState.value.alreadySent = invoice.value.dateSent !== undefined;
      uiState.value.status = fullInvoice.status;
    }
  };

  const saveInvoice = async () => {
    const request: EditInvoiceDto = {
      ...(invoice.value as EditInvoiceDto),
      datePaid: uiState.value.alreadyPaid ? invoice.value.datePaid : undefined,
      dateSent: uiState.value.alreadySent ? invoice.value.dateSent : undefined,
    };
    if (props.id) {
      await invoicesApi.updateInvoice({
        workspaceId: currentWorkspaceId,
        editInvoiceDto: request,
        invoiceId: ensureDefined(props.id),
      });
    } else {
      await invoicesApi.createInvoice({
        workspaceId: currentWorkspaceId,
        editInvoiceDto: request,
      });
    }
    await navigateToInvoicesOverview();
  };

  const {
    formRef,
    submitForm,
    documentsUploadRef,
    onDocumentsUploadComplete,
    onDocumentsUploadFailure,
    executeWithFormBlocked,
  } = useFormWithDocumentsUpload(loadInvoice, saveInvoice);

  const pageHeader = props.id === undefined
    ? $t.value.editInvoice.pageHeader.create()
    : $t.value.editInvoice.pageHeader.edit();

  const cancelInvoice = useConfirmation(
    $t.value.editInvoice.cancelInvoice.confirm.message(),
    {
      title: 'Warning',
      confirmButtonText: $t.value.editInvoice.cancelInvoice.confirm.yes(),
      cancelButtonText: $t.value.editInvoice.cancelInvoice.confirm.no(),
      type: 'warning',
    },
    async () => executeWithFormBlocked(async () => {
      const updatedInvoice = await invoicesApi.cancelInvoice({
        workspaceId: currentWorkspaceId,
        invoiceId: ensureDefined(props.id),
      });
      invoice.value = updatedInvoice;
      uiState.value.status = updatedInvoice.status;
    }),
  );
</script>
