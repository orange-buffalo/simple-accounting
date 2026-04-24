<template>
  <div>
    <div class="sa-page-header">
      <h1>{{ pageHeader }}</h1>
    </div>

    <SaForm v-model="formValues" :on-submit="saveTaxPayment" :on-load="loadTaxPayment" :on-cancel="navigateToTaxPaymentsOverview">
      <div class="row">
        <div class="col col-xs-12 col-lg-6">
          <h2>{{ $t.editIncomeTaxPayment.generalInformation.header() }}</h2>

          <SaFormInput
            prop="title"
            :label="$t.editIncomeTaxPayment.generalInformation.title.label()"
            :placeholder="$t.editIncomeTaxPayment.generalInformation.title.placeholder()"
          />

          <SaFormMoneyInput
            prop="amount"
            :label="$t.editIncomeTaxPayment.generalInformation.amount.label()"
            :currency="defaultCurrency"
          />

          <SaFormDatePickerInput
            prop="datePaid"
            :label="$t.editIncomeTaxPayment.generalInformation.datePaid.label()"
            :placeholder="$t.editIncomeTaxPayment.generalInformation.datePaid.placeholder()"
          />

          <SaFormDatePickerInput
            prop="reportingDate"
            :label="$t.editIncomeTaxPayment.generalInformation.reportingDate.label()"
            :placeholder="$t.editIncomeTaxPayment.generalInformation.reportingDate.placeholder()"
          />
        </div>

        <div class="col col-xs-12 col-lg-6">
          <h2>{{ $t.editIncomeTaxPayment.additionalInformation.header() }}</h2>

          <SaFormNotesInput
            prop="notes"
            :label="$t.editIncomeTaxPayment.additionalInformation.notes.label()"
            :placeholder="$t.editIncomeTaxPayment.additionalInformation.notes.placeholder()"
          />

          <h2>{{ $t.editIncomeTaxPayment.attachments.header() }}</h2>

          <SaDocumentsUpload
            ref="documentsUploadRef"
            :documents="resolvedDocuments"
            :loading-on-create="id !== undefined"
            @update:documents-ids="formValues.attachments = $event"
            @uploads-completed="onDocumentsUploadComplete"
            @uploads-failed="onDocumentsUploadFailure"
          />
        </div>
      </div>
    </SaForm>
  </div>
</template>

<script lang="ts" setup>
  import { computed, ref } from 'vue';
  import { $t } from '@/services/i18n';
  import SaDocumentsUpload from '@/components/documents/SaDocumentsUpload.vue';
  import SaForm from '@/components/form/SaForm.vue';
  import SaFormInput from '@/components/form/SaFormInput.vue';
  import SaFormMoneyInput from '@/components/form/SaFormMoneyInput.vue';
  import SaFormDatePickerInput from '@/components/form/SaFormDatePickerInput.vue';
  import SaFormNotesInput from '@/components/form/SaFormNotesInput.vue';
  import useNavigation from '@/services/use-navigation';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import { ClientSideValidationError } from '@/components/form/sa-form-api';
  import { formatDateToLocalISOString } from '@/services/date-utils';
  import { graphql } from '@/services/api/gql';
  import { useMutation, useLazyQuery } from '@/services/api/use-gql-api.ts';
  import { useDocumentAttachments } from '@/components/documents/documents-gql-types';
  import useNotifications from '@/components/notifications/use-notifications.ts';

  const props = defineProps<{
    id?: number,
  }>();

  const { navigateByViewName } = useNavigation();
  const navigateToTaxPaymentsOverview = async () => navigateByViewName('income-tax-payments-overview');

  const { defaultCurrency, currentWorkspaceId } = useCurrentWorkspace();
  const { showErrorNotification } = useNotifications();

  type TaxPaymentFormValues = {
    title: string | null,
    datePaid: string | null,
    reportingDate: string | null,
    amount: number | undefined,
    notes: string | null,
    attachments: number[],
  };

  const formValues = ref<TaxPaymentFormValues>({
    title: null,
    datePaid: formatDateToLocalISOString(new Date()),
    reportingDate: null,
    amount: undefined,
    notes: null,
    attachments: [],
  });

  const { resolvedDocuments, setDocuments } = useDocumentAttachments();

  const getIncomeTaxPaymentQuery = useLazyQuery(graphql(`
    query getIncomeTaxPaymentForEdit($workspaceId: Long!, $id: Long!) {
      workspace(id: $workspaceId) {
        incomeTaxPayment(id: $id) {
          id
          title
          datePaid
          reportingDate
          amount
          notes
          attachments {
            ...DocumentData
          }
        }
      }
    }
  `), 'workspace');

  const loadTaxPayment = props.id !== undefined ? async () => {
    const workspace = await getIncomeTaxPaymentQuery({
      workspaceId: currentWorkspaceId,
      id: props.id!,
    });
    const loaded = workspace?.incomeTaxPayment;
    if (loaded) {
      formValues.value = {
        title: loaded.title,
        datePaid: loaded.datePaid,
        reportingDate: loaded.reportingDate ?? null,
        amount: loaded.amount,
        notes: loaded.notes ?? null,
        attachments: setDocuments(loaded.attachments),
      };
    }
  } : undefined;

  const createIncomeTaxPaymentMutation = useMutation(graphql(`
    mutation createIncomeTaxPaymentMutation(
      $workspaceId: Long!,
      $title: String!,
      $datePaid: LocalDate!,
      $reportingDate: LocalDate,
      $amount: Long!,
      $notes: String,
      $attachments: [Long!],
    ) {
      createIncomeTaxPayment(
        workspaceId: $workspaceId,
        title: $title,
        datePaid: $datePaid,
        reportingDate: $reportingDate,
        amount: $amount,
        notes: $notes,
        attachments: $attachments,
      ) {
        id
      }
    }
  `), 'createIncomeTaxPayment');

  const editIncomeTaxPaymentMutation = useMutation(graphql(`
    mutation editIncomeTaxPaymentMutation(
      $workspaceId: Long!,
      $id: Long!,
      $title: String!,
      $datePaid: LocalDate!,
      $reportingDate: LocalDate,
      $amount: Long!,
      $notes: String,
      $attachments: [Long!],
    ) {
      editIncomeTaxPayment(
        workspaceId: $workspaceId,
        id: $id,
        title: $title,
        datePaid: $datePaid,
        reportingDate: $reportingDate,
        amount: $amount,
        notes: $notes,
        attachments: $attachments,
      ) {
        id
      }
    }
  `), 'editIncomeTaxPayment');

  const documentsUploadRef = ref<InstanceType<typeof SaDocumentsUpload> | null>(null);
  let resolveUploads: (() => void) | null = null;
  let rejectUploads: ((err: Error) => void) | null = null;

  const onDocumentsUploadComplete = () => {
    resolveUploads?.();
    resolveUploads = null;
    rejectUploads = null;
  };

  const onDocumentsUploadFailure = () => {
    rejectUploads?.(new Error($t.value.useDocumentsUpload.documentsUploadFailure()));
    resolveUploads = null;
    rejectUploads = null;
  };

  const saveTaxPayment = async () => {
    if (!formValues.value.datePaid) {
      throw new ClientSideValidationError([{
        field: 'datePaid',
        message: $t.value.formValidationMessages.notNull(),
      }]);
    }

    const datePaid = formValues.value.datePaid;

    const uploadsPromise = new Promise<void>((resolve, reject) => {
      resolveUploads = resolve;
      rejectUploads = reject;
    });
    documentsUploadRef.value?.submitUploads();

    try {
      await uploadsPromise;
    } catch (e) {
      showErrorNotification((e as Error).message);
      return;
    }

    const values = formValues.value;
    if (props.id) {
      await editIncomeTaxPaymentMutation({
        workspaceId: currentWorkspaceId,
        id: props.id,
        title: values.title ?? '',
        datePaid,
        reportingDate: values.reportingDate ?? null,
        amount: values.amount ?? 0,
        notes: values.notes ?? null,
        attachments: values.attachments,
      });
    } else {
      await createIncomeTaxPaymentMutation({
        workspaceId: currentWorkspaceId,
        title: values.title ?? '',
        datePaid,
        reportingDate: values.reportingDate ?? null,
        amount: values.amount ?? 0,
        notes: values.notes ?? null,
        attachments: values.attachments,
      });
    }
    await navigateToTaxPaymentsOverview();
  };

  const pageHeader = computed(() => props.id !== undefined
    ? $t.value.editIncomeTaxPayment.header.edit()
    : $t.value.editIncomeTaxPayment.header.create());
</script>
