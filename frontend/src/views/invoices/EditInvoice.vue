<template>
  <div>
    <div class="sa-page-header">
      <h1>{{ pageHeader }}</h1>

      <div
        v-if="isEditing"
        class="sa-header-options"
      >
        <div>
          <span v-if="!invoice.dateCancelled">Recorded on {{ timeRecorded }}</span>
          <span v-if="invoice.dateCancelled">Cancelled on {{ dateCancelled }}</span>
        </div>

        <div>
          <ElButton type="danger">
            Delete
          </ElButton>
          <ElButton
            v-if="!invoice.dateCancelled"
            type="danger"
            @click="cancelInvoice"
          >
            Cancel Invoice
          </ElButton>
        </div>
      </div>
    </div>

    <div class="sa-form">
      <ElForm
        ref="invoiceForm"
        :model="invoice"
        label-position="right"
        label-width="200px"
        :rules="invoiceValidationRules"
      >
        <div class="row">
          <div class="col col-xs-12 col-lg-6">
            <h2>General Information</h2>

            <ElFormItem
              label="Customer"
              prop="customer"
            >
              <ElSelect
                v-model="invoice.customer"
                placeholder="Select a customer"
              >
                <ElOption
                  v-for="customer in customers"
                  :key="customer.id"
                  :label="customer.name"
                  :value="customer.id"
                />
              </ElSelect>
            </ElFormItem>

            <ElFormItem
              label="Description / Title"
              prop="title"
            >
              <ElInput
                v-model="invoice.title"
                placeholder="Provide a short summary"
              />
            </ElFormItem>

            <ElFormItem
              label="Currency"
              prop="currency"
            >
              <CurrencyInput v-model="invoice.currency" />
            </ElFormItem>

            <ElFormItem
              label="Amount"
              prop="amount"
            >
              <MoneyInput
                v-model="invoice.amount"
                :currency="invoice.currency"
              />
            </ElFormItem>

            <ElFormItem
              label="Date Issued"
              prop="dateIssued"
            >
              <!-- todo #78: format from cldr https://github.com/ElemeFE/element/issues/11353 -->
              <ElDatePicker
                v-model="invoice.dateIssued"
                type="date"
                placeholder="Date invoice is issued"
                value-format="yyyy-MM-dd"
              />
            </ElFormItem>

            <ElFormItem
              label="Due Date"
              prop="dueDate"
            >
              <!-- todo #78: format from cldr https://github.com/ElemeFE/element/issues/11353 -->
              <ElDatePicker
                v-model="invoice.dueDate"
                type="date"
                placeholder="Date invoice is due"
                value-format="yyyy-MM-dd"
              />
            </ElFormItem>

            <ElFormItem
              label="Included General Tax"
              prop="generalTax"
            >
              <ElSelect
                v-model="invoice.generalTax"
                clearable
                placeholder="Select a tax"
              >
                <ElOption
                  v-for="tax in generalTaxes"
                  :key="tax.id"
                  :label="tax.title"
                  :value="tax.id"
                />
              </ElSelect>
            </ElFormItem>

            <ElFormItem>
              <ElCheckbox v-model="alreadySent">
                Already Sent
              </ElCheckbox>
            </ElFormItem>

            <ElFormItem
              v-if="alreadySent"
              label="Date Sent"
              prop="dateSent"
            >
              <!-- todo #78: format from cldr https://github.com/ElemeFE/element/issues/11353 -->
              <ElDatePicker
                v-model="invoice.dateSent"
                type="date"
                placeholder="Date invoice is sent"
                value-format="yyyy-MM-dd"
              />
            </ElFormItem>

            <ElFormItem>
              <ElCheckbox v-model="alreadyPaid">
                Already Paid
              </ElCheckbox>
            </ElFormItem>

            <ElFormItem
              v-if="alreadyPaid"
              label="Date Paid"
              prop="datePaid"
            >
              <!-- todo #78: format from cldr https://github.com/ElemeFE/element/issues/11353 -->
              <ElDatePicker
                v-model="invoice.datePaid"
                type="date"
                placeholder="Date invoice is paid"
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
                v-model="invoice.notes"
                type="textarea"
                placeholder="Any additional information to be stored for this invoice record"
                rows="5"
              />
            </ElFormItem>

            <SaMarkdownOutput
              v-if="invoice.notes"
              :source="invoice.notes"
              preview
            />

            <h2>Attachments</h2>

            <DocumentsUpload
              ref="documentsUpload"
              v-model="invoice.uploads"
              form-property="uploads"
            />
          </div>
        </div>

        <hr>

        <div class="sa-buttons-bar">
          <ElButton @click="navigateToInvoicesOverview">
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
  import { assign, isNil } from 'lodash';
  import api from '@/services/api';
  import DocumentsUpload from '@/components/DocumentsUpload';
  import CurrencyInput from '@/components/CurrencyInput';
  import MoneyInput from '@/components/MoneyInput';
  import { UploadsInfo } from '@/components/uploads-info';
  import withMediumDateFormatter from '@/components/mixins/with-medium-date-formatter';
  import withMediumDateTimeFormatter from '@/components/mixins/with-medium-datetime-formatter';
  import { withCustomers } from '@/components/mixins/with-customers';
  import withGeneralTaxes from '@/components/mixins/with-general-taxes';
  import SaMarkdownOutput from '@/components/SaMarkdownOutput';

  export default {
    name: 'EditInvoice',

    components: {
      DocumentsUpload,
      CurrencyInput,
      MoneyInput,
      SaMarkdownOutput,
    },

    mixins: [withMediumDateFormatter, withMediumDateTimeFormatter, withCustomers, withGeneralTaxes],

    data() {
      return {
        invoice: {
          customer: null,
          title: null,
          currency: null,
          amount: null,
          attachments: [],
          notes: null,
          dateIssued: new Date(),
          dueDate: null,
          datePaid: null,
          dateSent: null,
          uploads: new UploadsInfo(),
          generalTax: null,
        },
        invoiceValidationRules: {
          customer: { required: true, message: 'Please select a customer' },
          currency: { required: true, message: 'Please select a currency' },
          title: { required: true, message: 'Please provide the title' },
          amount: { required: true, message: 'Please provide invoice amount' },
          dateIssued: { required: true, message: 'Please provide the date when invoice is issued' },
          dueDate: { required: true, message: 'Please provide the date when invoice is due' },
          dateSent: { required: true, message: 'Please provide the date when invoice is sent' },
          datePaid: { required: true, message: 'Please provide the date when invoice is paid' },
        },
        alreadySent: false,
        alreadyPaid: false,
      };
    },

    async created() {
      if (this.isEditing) {
        const invoiceResponse = await api.get(`/workspaces/${this.workspace.id}/invoices/${this.$route.params.id}`);
        this.invoice = assign({}, this.invoice, invoiceResponse.data);
        this.alreadyPaid = !isNil(this.invoice.datePaid);
        this.alreadySent = !isNil(this.invoice.dateSent);

        if (this.invoice.attachments && this.invoice.attachments.length) {
          const attachments = await api.pageRequest(`/workspaces/${this.workspace.id}/documents`)
            .eager()
            .eqFilter('id', this.invoice.attachments)
            .getPageData();
          attachments.forEach(attachment => this.invoice.uploads.add(attachment));
        }
      }
    },

    computed: {
      ...mapState('workspaces', {
        workspace: 'currentWorkspace',
      }),

      pageHeader() {
        return this.isEditing ? 'Edit Invoice' : 'Create New Invoice';
      },

      isEditing() {
        return !isNil(this.$route.params.id);
      },

      timeRecorded() {
        return this.invoice.timeRecorded ? this.mediumDateTimeFormatter(new Date(this.invoice.timeRecorded)) : '';
      },

      dateCancelled() {
        return this.invoice.dateCancelled ? this.mediumDateFormatter(new Date(this.invoice.dateCancelled)) : '';
      },
    },

    methods: {
      navigateToInvoicesOverview() {
        this.$router.push({ name: 'invoices-overview' });
      },

      async save() {
        try {
          await this.$refs.invoiceForm.validate();
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

        this.pushInvoice();
      },

      async cancelInvoice() {
        try {
          await this.$confirm('This will permanently cancel this invoice. Continue?', 'Warning', {
            confirmButtonText: 'Yes',
            cancelButtonText: 'No',
            type: 'warning',
          });
        } catch (e) {
          return;
        }

        this.invoice.dateCancelled = api.dateToString(new Date());
        this.pushInvoice();
      },

      async pushInvoice() {
        const invoiceToPush = {
          customer: this.invoice.customer,
          dateIssued: this.invoice.dateIssued,
          datePaid: this.alreadyPaid ? this.invoice.datePaid : null,
          dateSent: this.alreadySent ? this.invoice.dateSent : null,
          dateCancelled: this.invoice.dateCancelled,
          dueDate: this.invoice.dueDate,
          title: this.invoice.title,
          currency: this.invoice.currency,
          amount: this.invoice.amount,
          attachments: this.invoice.uploads.getDocumentsIds(),
          notes: this.invoice.notes,
          generalTax: this.invoice.generalTax,
        };

        if (this.isEditing) {
          await api.put(`/workspaces/${this.workspace.id}/invoices/${this.invoice.id}`, invoiceToPush);
        } else {
          await api.post(`/workspaces/${this.workspace.id}/invoices`, invoiceToPush);
        }

        this.$router.push({ name: 'invoices-overview' });
      },
    },
  };
</script>
