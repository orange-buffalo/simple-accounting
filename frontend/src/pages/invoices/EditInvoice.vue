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
                :documents="resolvedDocuments"
                :loading-on-create="id !== undefined"
                @update:documents-ids="invoice.attachments = $event"
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
  import { formatDateToLocalISOString } from '@/services/date-utils';
  import SaStatusLabel from '@/components/SaStatusLabel.vue';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import { useConfirmation } from '@/components/confirmation/use-confirmation';
  import { ensureDefined } from '@/services/utils';
  import { graphql } from '@/services/api/gql';
  import { useLazyQuery, useMutation } from '@/services/api/use-gql-api.ts';
  import type { InvoiceStatus } from '@/services/api/gql/graphql';
  import { useDocumentAttachments } from '@/components/documents/documents-gql-types';

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

  type InvoiceFormValues = {
    customer?: number,
    title?: string,
    dateIssued?: string,
    dateSent?: string,
    datePaid?: string,
    dueDate?: string,
    currency: string,
    amount?: number,
    notes?: string,
    generalTax?: number,
    attachments: number[],
  };

  const invoice = ref<InvoiceFormValues>({
    attachments: [],
    dateIssued: formatDateToLocalISOString(new Date()),
    currency: defaultCurrency,
  });

  const { resolvedDocuments, setDocuments } = useDocumentAttachments();

  const uiState = ref<{
    alreadySent: boolean,
    alreadyPaid: boolean,
    isEditing: boolean,
    status?: InvoiceStatus
  }>({
    alreadySent: false,
    alreadyPaid: false,
    isEditing: props.id != null,
  });

  const getInvoiceQuery = useLazyQuery(graphql(`
    query getInvoiceForEdit($workspaceId: Long!, $invoiceId: Long!) {
      workspace(id: $workspaceId) {
        invoice(id: $invoiceId) {
          id
          title
          dateIssued
          dateSent
          datePaid
          dueDate
          currency
          amount
          notes
          status
          customer {
            id
          }
          generalTax {
            id
          }
          attachments {
            ...DocumentData
          }
        }
      }
    }
  `), 'workspace');

  const loadInvoice = async () => {
    if (props.id !== undefined) {
      const workspace = await getInvoiceQuery({
        workspaceId: currentWorkspaceId,
        invoiceId: props.id,
      });
      const loaded = workspace?.invoice;
      if (loaded) {
        invoice.value = {
          customer: loaded.customer!.id,
          title: loaded.title,
          dateIssued: loaded.dateIssued,
          dateSent: loaded.dateSent ?? undefined,
          datePaid: loaded.datePaid ?? undefined,
          dueDate: loaded.dueDate,
          currency: loaded.currency,
          amount: loaded.amount,
          notes: loaded.notes ?? undefined,
          generalTax: loaded.generalTax?.id ?? undefined,
          attachments: setDocuments(loaded.attachments),
        };
        uiState.value.alreadyPaid = loaded.datePaid != null;
        uiState.value.alreadySent = loaded.dateSent != null;
        uiState.value.status = loaded.status;
      }
    }
  };

  const createInvoiceMutation = useMutation(graphql(`
    mutation createInvoiceMutation(
      $workspaceId: Long!,
      $customerId: Long!,
      $title: String!,
      $dateIssued: LocalDate!,
      $dateSent: LocalDate,
      $datePaid: LocalDate,
      $dueDate: LocalDate!,
      $currency: String!,
      $amount: Long!,
      $notes: String,
      $attachments: [Long!],
      $generalTaxId: Long
    ) {
      createInvoice(
        workspaceId: $workspaceId,
        customerId: $customerId,
        title: $title,
        dateIssued: $dateIssued,
        dateSent: $dateSent,
        datePaid: $datePaid,
        dueDate: $dueDate,
        currency: $currency,
        amount: $amount,
        notes: $notes,
        attachments: $attachments,
        generalTaxId: $generalTaxId
      ) {
        id
      }
    }
  `), 'createInvoice');

  const editInvoiceMutation = useMutation(graphql(`
    mutation editInvoiceMutation(
      $workspaceId: Long!,
      $id: Long!,
      $customerId: Long!,
      $title: String!,
      $dateIssued: LocalDate!,
      $dateSent: LocalDate,
      $datePaid: LocalDate,
      $dueDate: LocalDate!,
      $currency: String!,
      $amount: Long!,
      $notes: String,
      $attachments: [Long!],
      $generalTaxId: Long
    ) {
      editInvoice(
        workspaceId: $workspaceId,
        id: $id,
        customerId: $customerId,
        title: $title,
        dateIssued: $dateIssued,
        dateSent: $dateSent,
        datePaid: $datePaid,
        dueDate: $dueDate,
        currency: $currency,
        amount: $amount,
        notes: $notes,
        attachments: $attachments,
        generalTaxId: $generalTaxId
      ) {
        id
      }
    }
  `), 'editInvoice');

  const cancelInvoiceMutation = useMutation(graphql(`
    mutation cancelInvoiceMutation($workspaceId: Long!, $invoiceId: Long!) {
      cancelInvoice(workspaceId: $workspaceId, invoiceId: $invoiceId) {
        id
        status
      }
    }
  `), 'cancelInvoice');

  const saveInvoice = async () => {
    const datePaid = uiState.value.alreadyPaid ? (invoice.value.datePaid ?? null) : null;
    const dateSent = uiState.value.alreadySent ? (invoice.value.dateSent ?? null) : null;
    if (props.id) {
      await editInvoiceMutation({
        workspaceId: currentWorkspaceId,
        id: ensureDefined(props.id),
        customerId: invoice.value.customer!,
        title: invoice.value.title!,
        dateIssued: invoice.value.dateIssued!,
        dateSent,
        datePaid,
        dueDate: invoice.value.dueDate!,
        currency: invoice.value.currency,
        amount: invoice.value.amount!,
        notes: invoice.value.notes ?? null,
        attachments: invoice.value.attachments,
        generalTaxId: invoice.value.generalTax ?? null,
      });
    } else {
      await createInvoiceMutation({
        workspaceId: currentWorkspaceId,
        customerId: invoice.value.customer!,
        title: invoice.value.title!,
        dateIssued: invoice.value.dateIssued!,
        dateSent,
        datePaid,
        dueDate: invoice.value.dueDate!,
        currency: invoice.value.currency,
        amount: invoice.value.amount!,
        notes: invoice.value.notes ?? null,
        attachments: invoice.value.attachments,
        generalTaxId: invoice.value.generalTax ?? null,
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
      const result = await cancelInvoiceMutation({
        workspaceId: currentWorkspaceId,
        invoiceId: ensureDefined(props.id),
      });
      uiState.value.status = result.status;
    }),
  );
</script>
