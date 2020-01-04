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
            <h2>{{ $t('editIncomeTaxPayment.generalInformation.header') }}</h2>

            <ElFormItem
              :label="$t('editIncomeTaxPayment.generalInformation.title.label')"
              prop="title"
            >
              <ElInput
                v-model="taxPayment.title"
                :placeholder="$t('editIncomeTaxPayment.generalInformation.title.placeholder')"
              />
            </ElFormItem>

            <ElFormItem
              :label="$t('editIncomeTaxPayment.generalInformation.amount.label')"
              prop="amount"
            >
              <MoneyInput
                v-model="taxPayment.amount"
                :currency="defaultCurrency"
              />
            </ElFormItem>

            <ElFormItem
              :label="$t('editIncomeTaxPayment.generalInformation.datePaid.label')"
              prop="datePaid"
            >
              <!-- todo #78: format from cldr https://github.com/ElemeFE/element/issues/11353 -->
              <ElDatePicker
                v-model="taxPayment.datePaid"
                type="date"
                :placeholder="$t('editIncomeTaxPayment.generalInformation.datePaid.placeholder')"
                value-format="yyyy-MM-dd"
              />
            </ElFormItem>

            <ElFormItem
              :label="$t('editIncomeTaxPayment.generalInformation.reportingDate.label')"
              prop="reportingDate"
            >
              <!-- todo #78: format from cldr https://github.com/ElemeFE/element/issues/11353 -->
              <ElDatePicker
                v-model="taxPayment.reportingDate"
                type="date"
                :placeholder="$t('editIncomeTaxPayment.generalInformation.reportingDate.placeholder')"
                value-format="yyyy-MM-dd"
              />
            </ElFormItem>
          </div>

          <div class="col col-xs-12 col-lg-6">
            <h2>{{ $t('editIncomeTaxPayment.additionalInformation.header') }}</h2>

            <ElFormItem
              :label="$t('editIncomeTaxPayment.additionalInformation.notes.label')"
              prop="notes"
            >
              <SaNotesInput
                v-model="taxPayment.notes"
                :placeholder="$t('editIncomeTaxPayment.additionalInformation.notes.placeholder')"
              />
            </ElFormItem>

            <h2>{{ $t('editIncomeTaxPayment.attachments.header') }}</h2>

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
          {{ $t('editIncomeTaxPayment.cancel') }}
        </ElButton>
        <ElButton
          type="primary"
          @click="save"
        >
          {{ $t('editIncomeTaxPayment.save') }}
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
            message: this.$t('editIncomeTaxPayment.validations.title'),
          },
          datePaid: {
            required: true,
            message: this.$t('editIncomeTaxPayment.validations.datePaid'),
          },
          amount: {
            required: true,
            message: this.$t('editIncomeTaxPayment.validations.amount'),
          },
        },
      };
    },

    computed: {
      pageHeader() {
        return this.$route.params.id
          ? this.$t('editIncomeTaxPayment.header.edit')
          : this.$t('editIncomeTaxPayment.header.create');
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
          message: this.$t('editIncomeTaxPayment.uploadFailure'),
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
