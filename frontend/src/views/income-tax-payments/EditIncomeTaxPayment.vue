<template>
  <div>
    <div class="sa-page-header">
      <h1>{{ pageHeader }}</h1>
    </div>

    <div class="sa-form">
      <ElForm
        ref="taxPaymentForm"
        :model="taxPayment"
        label-position="right"
        label-width="200px"
        :rules="taxPaymentValidationRules"
      >
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
              <ElInput
                v-model="taxPayment.notes"
                type="textarea"
                placeholder="Any additional information to be stored for this tax payment record"
                rows="5"
              />
            </ElFormItem>

            <SaMarkdownOutput
              v-if="taxPayment.notes"
              :source="taxPayment.notes"
              preview
            />

            <h2>Attachments</h2>

            <DocumentsUpload
              ref="documentsUpload"
              v-model="taxPayment.uploads"
              form-property="uploads"
            />
          </div>
        </div>

        <hr>

        <div class="sa-buttons-bar">
          <ElButton @click="navigateToTaxPaymentsOverview">
            Cancel
          </ElButton>
          <ElButton
            type="primary"
            @click="save"
          >
            Save
          </ElButton>
        </div>
      </ElForm>
    </div>
  </div>
</template>

<script>
  import { mapState } from 'vuex';
  import { assign } from 'lodash';
  import api from '@/services/api';
  import DocumentsUpload from '@/components/DocumentsUpload';
  import MoneyInput from '@/components/MoneyInput';
  import { UploadsInfo } from '@/components/uploads-info';
  import withMediumDateFormatter from '@/components/mixins/with-medium-date-formatter';

  import SaMarkdownOutput from '@/components/SaMarkdownOutput';

  export default {
    name: 'EditIncomeTaxPayment',

    components: {
      DocumentsUpload,
      MoneyInput,
      SaMarkdownOutput,
    },

    mixins: [withMediumDateFormatter],

    data() {
      return {
        taxPayment: {
          title: null,
          amount: null,
          attachments: [],
          notes: null,
          datePaid: new Date(),
          uploads: new UploadsInfo(),
          reportingDate: null,
        },
        taxPaymentValidationRules: {
          title: { required: true, message: 'Please provide the title' },
          datePaid: { required: true, message: 'Please provide the date when tax payment is done' },
          amount: { required: true, message: 'Please provide tax payment amount' },
        },
      };
    },

    async created() {
      if (this.$route.params.id) {
        const taxPaymentResponse = await api.get(`/workspaces/${this.workspace.id}/income-tax-payments/${this.$route.params.id}`);
        this.taxPayment = assign({}, this.taxPayment, taxPaymentResponse.data);

        if (this.taxPayment.attachments && this.taxPayment.attachments.length) {
          const attachments = await api.pageRequest(`/workspaces/${this.workspace.id}/documents`)
            .eager()
            .eqFilter('id', this.taxPayment.attachments)
            .getPageData();
          attachments.forEach(attachment => this.taxPayment.uploads.add(attachment));
        }
      }
    },

    computed: {
      ...mapState('workspaces', {
        workspace: 'currentWorkspace',
      }),

      defaultCurrency() {
        return this.workspace.defaultCurrency;
      },

      pageHeader() {
        return this.$route.params.id ? 'Edit Income Tax Payment' : 'Record New Income Tax Payment';
      },
    },

    methods: {
      navigateToTaxPaymentsOverview() {
        this.$router.push({ name: 'income-tax-payments-overview' });
      },

      async save() {
        try {
          await this.$refs.taxPaymentForm.validate();
        } catch (e) {
          return;
        }

        try {
          await this.$refs.documentsUpload.submitUploads();
        } catch (e) {
          this.$message({
            showClose: true,
            message: 'Upload failed',
            type: 'error',
          });
          return;
        }

        const taxPaymentToPush = {
          datePaid: this.taxPayment.datePaid,
          title: this.taxPayment.title,
          amount: this.taxPayment.amount,
          attachments: this.taxPayment.uploads.getDocumentsIds(),
          notes: this.taxPayment.notes,
          reportingDate: this.taxPayment.reportingDate,
        };

        if (this.taxPayment.id) {
          await api.put(`/workspaces/${this.workspace.id}/income-tax-payments/${this.taxPayment.id}`, taxPaymentToPush);
        } else {
          await api.post(`/workspaces/${this.workspace.id}/income-tax-payments`, taxPaymentToPush);
        }
        await this.$router.push({ name: 'income-tax-payments-overview' });
      },
    },
  };
</script>
