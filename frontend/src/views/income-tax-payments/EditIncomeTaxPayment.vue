<template>
  <div>
    <div class="sa-page-header">
      <h1>{{ pageHeader }}</h1>
    </div>

    <SaForm
      ref="taxPaymentForm"
      :model="taxPayment"
      :rules="taxPaymentValidationRules"
    >
      <template #default>
        <div class="row">
          <div class="col col-xs-12 col-lg-6">
            <h2>General Information</h2>

            <ElFormItem
              label="Description / Title"
              prop="title"
            >
              <ElInput
                v-model="taxPayment.title"
                placeholder="Provide a short summary"
              />
            </ElFormItem>

            <ElFormItem
              label="Amount"
              prop="amount"
            >
              <MoneyInput
                v-model="taxPayment.amount"
                :currency="defaultCurrency"
              />
            </ElFormItem>

            <ElFormItem
              label="Date Paid"
              prop="datePaid"
            >
              <!-- todo #78: format from cldr https://github.com/ElemeFE/element/issues/11353 -->
              <ElDatePicker
                v-model="taxPayment.datePaid"
                type="date"
                placeholder="Date tax is paid"
                value-format="yyyy-MM-dd"
              />
            </ElFormItem>

            <ElFormItem
              label="Reporting Date"
              prop="reportingDate"
            >
              <!-- todo #78: format from cldr https://github.com/ElemeFE/element/issues/11353 -->
              <ElDatePicker
                v-model="taxPayment.reportingDate"
                type="date"
                placeholder="Date to include this payment into reporting by"
                value-format="yyyy-MM-dd"
              />
            </ElFormItem>
          </div>

          <div class="col col-xs-12 col-lg-6">
            <h2>Additional notes</h2>

            <ElFormItem
              label="Notes"
              prop="notes"
            >
              <SaNotesInput
                v-model="taxPayment.notes"
                placeholder="Any additional information to be stored for this tax payment record"
              />
            </ElFormItem>

            <h2>Attachments</h2>

            <ElFormItem>
              <SaDocumentsUpload
                ref="documentsUpload"
                :documents-ids="taxPayment.attachments"
                @uploads-completed="onDocumentsUploadSuccess"
                @uploads-failed="onDocumentsUploadFailure"
              />
            </ElFormItem>
          </div>
        </div>
      </template>

      <template #buttons-bar>
        <ElButton @click="navigateToTaxPaymentsOverview">
          Cancel
        </ElButton>
        <ElButton
          type="primary"
          @click="save"
        >
          Save
        </ElButton>
      </template>
    </SaForm>
  </div>
</template>

<script>
  import { api } from '@/services/api';
  import MoneyInput from '@/components/MoneyInput';
  import SaDocumentsUpload from '@/components/documents/SaDocumentsUpload';
  import SaNotesInput from '@/components/SaNotesInput';
  import SaForm from '@/components/SaForm';
  import withWorkspaces from '@/components/mixins/with-workspaces';

  export default {
    name: 'EditIncomeTaxPayment',

    components: {
      SaForm,
      SaNotesInput,
      SaDocumentsUpload,
      MoneyInput,
    },

    mixins: [withWorkspaces],

    data() {
      return {
        taxPayment: {
          title: null,
          amount: null,
          attachments: [],
          notes: null,
          datePaid: new Date(),
          reportingDate: null,
        },
        taxPaymentValidationRules: {
          title: {
            required: true,
            message: 'Please provide the title',
          },
          datePaid: {
            required: true,
            message: 'Please provide the date when tax payment is done',
          },
          amount: {
            required: true,
            message: 'Please provide tax payment amount',
          },
        },
      };
    },

    computed: {
      pageHeader() {
        return this.$route.params.id ? 'Edit Income Tax Payment' : 'Record New Income Tax Payment';
      },
    },

    async created() {
      if (this.$route.params.id) {
        const taxPaymentResponse = await api
          .get(`/workspaces/${this.currentWorkspace.id}/income-tax-payments/${this.$route.params.id}`);
        this.taxPayment = { ...this.taxPayment, ...taxPaymentResponse.data };
      }
    },

    methods: {
      navigateToTaxPaymentsOverview() {
        this.$router.push({ name: 'income-tax-payments-overview' });
      },

      async onDocumentsUploadSuccess(documentsIds) {
        const taxPaymentToPush = {
          datePaid: this.taxPayment.datePaid,
          title: this.taxPayment.title,
          amount: this.taxPayment.amount,
          attachments: documentsIds,
          notes: this.taxPayment.notes,
          reportingDate: this.taxPayment.reportingDate,
        };

        if (this.taxPayment.id) {
          await api.put(
            `/workspaces/${this.currentWorkspace.id}/income-tax-payments/${this.taxPayment.id}`,
            taxPaymentToPush,
          );
        } else {
          await api.post(`/workspaces/${this.currentWorkspace.id}/income-tax-payments`, taxPaymentToPush);
        }
        await this.$router.push({ name: 'income-tax-payments-overview' });
      },

      async onDocumentsUploadFailure() {
        this.$message({
          showClose: true,
          message: 'Some of the documents have not been uploaded. Please retry or remove them.',
          type: 'error',
        });
      },

      async save() {
        try {
          await this.$refs.taxPaymentForm.validate();
        } catch (e) {
          return;
        }

        await this.$refs.documentsUpload.submitUploads();
      },
    },
  };
</script>
