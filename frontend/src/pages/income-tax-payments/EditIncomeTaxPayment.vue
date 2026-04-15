<template>
  <div>
    <div class="sa-page-header">
      <h1>{{ pageHeader }}</h1>
    </div>

    <SaLegacyForm
      ref="formRef"
      :model="taxPayment"
      :rules="taxPaymentValidationRules"
    >
      <template #default>
        <div class="row">
          <div class="col col-xs-12 col-lg-6">
            <h2>{{ $t.editIncomeTaxPayment.generalInformation.header() }}</h2>

            <ElFormItem
              :label="$t.editIncomeTaxPayment.generalInformation.title.label()"
              prop="title"
            >
              <ElInput
                v-model="taxPayment.title"
                :placeholder="$t.editIncomeTaxPayment.generalInformation.title.placeholder()"
              />
            </ElFormItem>

            <ElFormItem
              :label="$t.editIncomeTaxPayment.generalInformation.amount.label()"
              prop="amount"
            >
              <SaMoneyInput
                v-model="taxPayment.amount"
                :currency="defaultCurrency"
              />
            </ElFormItem>

            <ElFormItem
              :label="$t.editIncomeTaxPayment.generalInformation.datePaid.label()"
              prop="datePaid"
            >
              <!-- todo #78: format from cldr https://github.com/ElemeFE/element/issues/11353 -->
              <ElDatePicker
                v-model="taxPayment.datePaid"
                type="date"
                value-format="YYYY-MM-DD"
                :placeholder="$t.editIncomeTaxPayment.generalInformation.datePaid.placeholder()"
              />
            </ElFormItem>

            <ElFormItem
              :label="$t.editIncomeTaxPayment.generalInformation.reportingDate.label()"
              prop="reportingDate"
            >
              <!-- todo #78: format from cldr https://github.com/ElemeFE/element/issues/11353 -->
              <ElDatePicker
                v-model="taxPayment.reportingDate"
                type="date"
                value-format="YYYY-MM-DD"
                :placeholder="$t.editIncomeTaxPayment.generalInformation.reportingDate.placeholder()"
              />
            </ElFormItem>
          </div>

          <div class="col col-xs-12 col-lg-6">
            <h2>{{ $t.editIncomeTaxPayment.additionalInformation.header() }}</h2>

            <ElFormItem
              :label="$t.editIncomeTaxPayment.additionalInformation.notes.label()"
              prop="notes"
            >
              <SaNotesInput
                v-model="taxPayment.notes"
                :placeholder="$t.editIncomeTaxPayment.additionalInformation.notes.placeholder()"
              />
            </ElFormItem>

            <h2>{{ $t.editIncomeTaxPayment.attachments.header() }}</h2>

            <ElFormItem>
              <SaDocumentsUpload
                ref="documentsUploadRef"
                :documents="resolvedDocuments"
                :loading-on-create="id !== undefined"
                @update:documents-ids="taxPayment.attachments = $event"
                @uploads-completed="onDocumentsUploadComplete"
                @uploads-failed="onDocumentsUploadFailure"
              />
            </ElFormItem>
          </div>
        </div>
      </template>

      <template #buttons-bar>
        <ElButton @click="navigateToTaxPaymentsOverview">
          {{ $t.editIncomeTaxPayment.cancel() }}
        </ElButton>
        <ElButton
          type="primary"
          @click="submitForm"
        >
          {{ $t.editIncomeTaxPayment.save() }}
        </ElButton>
      </template>
    </SaLegacyForm>
  </div>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import { $t } from '@/services/i18n';
  import SaMoneyInput from '@/components/SaMoneyInput.vue';
  import SaDocumentsUpload from '@/components/documents/SaDocumentsUpload.vue';
  import SaNotesInput from '@/components/notes-input/SaNotesInput.vue';
  import SaLegacyForm from '@/components/form/SaLegacyForm.vue';
  import useNavigation from '@/services/use-navigation';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import { useFormWithDocumentsUpload } from '@/components/form/use-form';
  import { formatDateToLocalISOString } from '@/services/date-utils';
  import { graphql } from '@/services/api/gql';
  import { useMutation, useLazyQuery } from '@/services/api/use-gql-api.ts';
  import { useDocumentAttachments } from '@/components/documents/documents-gql-types';

  const props = defineProps<{
    id?: number,
  }>();

  const taxPaymentValidationRules = {
    title: {
      required: true,
      message: $t.value.editIncomeTaxPayment.validations.title(),
    },
    datePaid: {
      required: true,
      message: $t.value.editIncomeTaxPayment.validations.datePaid(),
    },
    amount: {
      required: true,
      message: $t.value.editIncomeTaxPayment.validations.amount(),
    },
  };

  const { navigateByViewName } = useNavigation();
  const navigateToTaxPaymentsOverview = async () => navigateByViewName('income-tax-payments-overview');

  const {
    defaultCurrency,
    currentWorkspaceId,
  } = useCurrentWorkspace();

  type TaxPaymentFormValues = {
    title?: string,
    datePaid: string,
    reportingDate?: string,
    amount?: number,
    notes?: string,
    attachments: Array<number>,
  };

  const taxPayment = ref<TaxPaymentFormValues>({
    datePaid: formatDateToLocalISOString(new Date()),
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

  const loadTaxPayment = async () => {
    if (props.id !== undefined) {
      const workspace = await getIncomeTaxPaymentQuery({
        workspaceId: currentWorkspaceId,
        id: props.id,
      });
      const loaded = workspace?.incomeTaxPayment;
      if (loaded) {
        taxPayment.value = {
          title: loaded.title,
          datePaid: loaded.datePaid,
          reportingDate: loaded.reportingDate,
          amount: loaded.amount,
          notes: loaded.notes ?? undefined,
          attachments: setDocuments(loaded.attachments),
        };
      }
    }
  };

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

  const saveTaxPayment = async () => {
    const values = taxPayment.value as Required<TaxPaymentFormValues>;
    if (props.id) {
      await editIncomeTaxPaymentMutation({
        workspaceId: currentWorkspaceId,
        id: props.id,
        title: values.title,
        datePaid: values.datePaid,
        reportingDate: values.reportingDate ?? null,
        amount: values.amount,
        notes: values.notes ?? null,
        attachments: values.attachments,
      });
    } else {
      await createIncomeTaxPaymentMutation({
        workspaceId: currentWorkspaceId,
        title: values.title,
        datePaid: values.datePaid,
        reportingDate: values.reportingDate ?? null,
        amount: values.amount,
        notes: values.notes ?? null,
        attachments: values.attachments,
      });
    }
    await navigateToTaxPaymentsOverview();
  };

  const {
    formRef,
    submitForm,
    documentsUploadRef,
    onDocumentsUploadComplete,
    onDocumentsUploadFailure,
  } = useFormWithDocumentsUpload(loadTaxPayment, saveTaxPayment);

  const pageHeader = props.id
    ? $t.value.editIncomeTaxPayment.header.edit()
    : $t.value.editIncomeTaxPayment.header.create();
</script>
