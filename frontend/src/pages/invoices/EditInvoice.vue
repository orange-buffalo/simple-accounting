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

    <SaForm
      v-model="formValues"
      :on-submit="saveInvoice"
      :on-load="loadInvoice"
      :on-cancel="navigateToInvoicesOverview"
    >
      <div class="row">
          <div class="col col-xs-12 col-lg-6">
            <h2>{{ $t.editInvoice.generalInformation.header() }}</h2>

            <SaFormCustomerInput
              prop="customerId"
              :label="$t.editInvoice.generalInformation.customer.label()"
              :placeholder="$t.editInvoice.generalInformation.customer.placeholder()"
            />

            <SaFormInput
              prop="title"
              :label="$t.editInvoice.generalInformation.title.label()"
              :placeholder="$t.editInvoice.generalInformation.title.placeholder()"
            />

            <SaFormCurrencyInput
              prop="currency"
              :label="$t.editInvoice.generalInformation.currency.label()"
            />

            <SaFormMoneyInput
              prop="amount"
              :label="$t.editInvoice.generalInformation.amount.label()"
              :currency="formValues.currency ?? defaultCurrency"
            />

            <SaFormDatePickerInput
              prop="dateIssued"
              :label="$t.editInvoice.generalInformation.dateIssued.label()"
              :placeholder="$t.editInvoice.generalInformation.dateIssued.placeholder()"
            />

            <SaFormDatePickerInput
              prop="dueDate"
              :label="$t.editInvoice.generalInformation.dueDate.label()"
              :placeholder="$t.editInvoice.generalInformation.dueDate.placeholder()"
            />

            <SaFormGeneralTaxInput
              prop="generalTaxId"
              :label="$t.editInvoice.generalInformation.generalTax.label()"
              :placeholder="$t.editInvoice.generalInformation.generalTax.placeholder()"
              clearable
            />

            <ElFormItem>
              <ElCheckbox v-model="uiState.alreadySent">
                {{ $t.editInvoice.generalInformation.alreadySent.label() }}
              </ElCheckbox>
            </ElFormItem>

            <SaFormDatePickerInput
              v-if="uiState.alreadySent"
              prop="dateSent"
              :label="$t.editInvoice.generalInformation.dateSent.label()"
              :placeholder="$t.editInvoice.generalInformation.dateSent.placeholder()"
            />

            <ElFormItem>
              <ElCheckbox v-model="uiState.alreadyPaid">
                {{ $t.editInvoice.generalInformation.alreadyPaid.label() }}
              </ElCheckbox>
            </ElFormItem>

            <SaFormDatePickerInput
              v-if="uiState.alreadyPaid"
              prop="datePaid"
              :label="$t.editInvoice.generalInformation.datePaid.label()"
              :placeholder="$t.editInvoice.generalInformation.datePaid.placeholder()"
            />
          </div>

          <div class="col col-xs-12 col-lg-6">
            <h2>{{ $t.editInvoice.additionalNotes.header() }}</h2>

            <SaFormNotesInput
              prop="notes"
              :label="$t.editInvoice.additionalNotes.notes.label()"
              :placeholder="$t.editInvoice.additionalNotes.notes.placeholder()"
            />

            <h2>{{ $t.editInvoice.attachments.header() }}</h2>

            <SaFormDocumentsUpload prop="attachments" :documents="resolvedDocuments" />
          </div>
        </div>
    </SaForm>
  </div>
</template>

<script lang="ts" setup>
  import { computed, ref } from 'vue';
  import { $t } from '@/services/i18n';
  import SaForm from '@/components/form/SaForm.vue';
  import SaFormInput from '@/components/form/SaFormInput.vue';
  import SaFormCustomerInput from '@/components/form/SaFormCustomerInput.vue';
  import SaFormCurrencyInput from '@/components/form/SaFormCurrencyInput.vue';
  import SaFormMoneyInput from '@/components/form/SaFormMoneyInput.vue';
  import SaFormDatePickerInput from '@/components/form/SaFormDatePickerInput.vue';
  import SaFormGeneralTaxInput from '@/components/form/SaFormGeneralTaxInput.vue';
  import SaFormNotesInput from '@/components/form/SaFormNotesInput.vue';
  import SaFormDocumentsUpload from '@/components/form/SaFormDocumentsUpload.vue';
  import useNavigation from '@/services/use-navigation';
  import { formatDateToLocalISOString } from '@/services/date-utils';
  import SaStatusLabel from '@/components/SaStatusLabel.vue';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import { useConfirmation } from '@/components/confirmation/use-confirmation';
  import { ensureDefined } from '@/services/utils';
  import { graphql } from '@/services/api/gql';
  import { useLazyQuery, useMutation } from '@/services/api/use-gql-api.ts';
  import type {
    CreateInvoiceMutationMutationVariables,
    EditInvoiceMutationMutationVariables,
    InvoiceStatus,
  } from '@/services/api/gql/graphql';
  import { useDocumentAttachments } from '@/components/documents/documents-gql-types';
  import { AsFormValues, toRequestArgs, updateFormValues } from '@/components/form/sa-form-api.ts';

  const props = defineProps<{
    id?: number
  }>();

  const { navigateByViewName } = useNavigation();
  const navigateToInvoicesOverview = async () => {
    await navigateByViewName('invoices-overview');
  };

  const {
    currentWorkspaceId,
    defaultCurrency,
  } = useCurrentWorkspace();

  type InvoiceFormValues = AsFormValues<[
    CreateInvoiceMutationMutationVariables,
    EditInvoiceMutationMutationVariables
  ]>;

  const formValues = ref<InvoiceFormValues>({
    id: props.id,
    workspaceId: currentWorkspaceId,
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

  const loadInvoice = props.id !== undefined ? async () => {
    const workspace = await getInvoiceQuery({
      workspaceId: currentWorkspaceId,
      invoiceId: props.id!,
    });
    const invoice = workspace?.invoice;
    if (invoice) {
      updateFormValues(formValues, invoice, loaded => ({
        id: loaded.id,
        customerId: loaded.customer!.id,
        generalTaxId: loaded.generalTax?.id ?? undefined,
        attachments: setDocuments(loaded.attachments),
      }));
      uiState.value.alreadyPaid = invoice.datePaid != null;
      uiState.value.alreadySent = invoice.dateSent != null;
      uiState.value.status = invoice.status;
    }
  } : undefined;

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
    formValues.value.datePaid = uiState.value.alreadyPaid ? (formValues.value.datePaid ?? null) : null;
    formValues.value.dateSent = uiState.value.alreadySent ? (formValues.value.dateSent ?? null) : null;
    if (props.id) {
      await editInvoiceMutation(toRequestArgs(formValues));
    } else {
      await createInvoiceMutation(toRequestArgs(formValues));
    }
    await navigateToInvoicesOverview();
  };

  const pageHeader = computed(() => props.id === undefined
    ? $t.value.editInvoice.pageHeader.create()
    : $t.value.editInvoice.pageHeader.edit());

  const cancelInvoice = useConfirmation(
    $t.value.editInvoice.cancelInvoice.confirm.message(),
    {
      title: 'Warning',
      confirmButtonText: $t.value.editInvoice.cancelInvoice.confirm.yes(),
      cancelButtonText: $t.value.editInvoice.cancelInvoice.confirm.no(),
      type: 'warning',
    },
    async () => {
      const result = await cancelInvoiceMutation({
        workspaceId: currentWorkspaceId,
        invoiceId: ensureDefined(props.id),
      });
      uiState.value.status = result.status;
    },
  );
</script>
