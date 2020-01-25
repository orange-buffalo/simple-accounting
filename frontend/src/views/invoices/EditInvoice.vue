<template>
  <div>
    <div class="sa-page-header">
      <h1>{{ pageHeader }}</h1>

      <div
        v-if="isEditing"
        class="sa-header-options"
      >
        <div>
          <span v-if="!invoice.dateCancelled">{{ $t('editInvoice.recordedOn', [invoice.timeRecorded]) }}</span>
          <span v-if="invoice.dateCancelled">{{ $t('editInvoice.cancelledOn', [invoice.dateCancelled]) }}</span>
        </div>

        <div>
          <ElButton
            v-if="!invoice.dateCancelled"
            type="danger"
            @click="cancelInvoice"
          >
            {{ $t('editInvoice.cancelInvoice.button') }}
          </ElButton>
        </div>
      </div>
    </div>

    <SaForm
      ref="invoiceForm"
      :model="invoice"
      :rules="invoiceValidationRules"
    >
      <template #default>
        <div class="row">
          <div class="col col-xs-12 col-lg-6">
            <h2>{{ $t('editInvoice.generalInformation.header') }}</h2>

            <ElFormItem
              :label="$t('editInvoice.generalInformation.customer.label')"
              prop="customer"
            >
              <SaCustomerInput
                v-model="invoice.customer"
                :placeholder="$t('editInvoice.generalInformation.customer.placeholder')"
              />
            </ElFormItem>

            <ElFormItem
              :label="$t('editInvoice.generalInformation.title.label')"
              prop="title"
            >
              <ElInput
                v-model="invoice.title"
                :placeholder="$t('editInvoice.generalInformation.title.placeholder')"
              />
            </ElFormItem>

            <ElFormItem
              :label="$t('editInvoice.generalInformation.currency.label')"
              prop="currency"
            >
              <SaCurrencyInput v-model="invoice.currency" />
            </ElFormItem>

            <ElFormItem
              :label="$t('editInvoice.generalInformation.amount.label')"
              prop="amount"
            >
              <MoneyInput
                v-model="invoice.amount"
                :currency="invoice.currency"
              />
            </ElFormItem>

            <ElFormItem
              :label="$t('editInvoice.generalInformation.dateIssued.label')"
              prop="dateIssued"
            >
              <!-- todo #78: format from cldr https://github.com/ElemeFE/element/issues/11353 -->
              <ElDatePicker
                v-model="invoice.dateIssued"
                type="date"
                :placeholder="$t('editInvoice.generalInformation.dateIssued.placeholder')"
                value-format="yyyy-MM-dd"
              />
            </ElFormItem>

            <ElFormItem
              :label="$t('editInvoice.generalInformation.dueDate.label')"
              prop="dueDate"
            >
              <!-- todo #78: format from cldr https://github.com/ElemeFE/element/issues/11353 -->
              <ElDatePicker
                v-model="invoice.dueDate"
                type="date"
                :placeholder="$t('editInvoice.generalInformation.dueDate.placeholder')"
                value-format="yyyy-MM-dd"
              />
            </ElFormItem>

            <ElFormItem
              :label="$t('editInvoice.generalInformation.generalTax.label')"
              prop="generalTax"
            >
              <ElSelect
                v-model="invoice.generalTax"
                clearable
                :placeholder="$t('editInvoice.generalInformation.generalTax.placeholder')"
              >
                <ElOption
                  v-for="tax in generalTaxes"
                  :key="tax.id"
                  ::label="tax.title"
                  :value="tax.id"
                />
              </ElSelect>
            </ElFormItem>

            <ElFormItem>
              <ElCheckbox v-model="alreadySent">
                {{ $t('editInvoice.generalInformation.alreadySent.label') }}
              </ElCheckbox>
            </ElFormItem>

            <ElFormItem
              v-if="alreadySent"
              :label="$t('editInvoice.generalInformation.dateSent.label')"
              prop="dateSent"
            >
              <!-- todo #78: format from cldr https://github.com/ElemeFE/element/issues/11353 -->
              <ElDatePicker
                v-model="invoice.dateSent"
                type="date"
                :placeholder="$t('editInvoice.generalInformation.dateSent.placeholder')"
                value-format="yyyy-MM-dd"
              />
            </ElFormItem>

            <ElFormItem>
              <ElCheckbox v-model="alreadyPaid">
                {{ $t('editInvoice.generalInformation.alreadyPaid.label') }}
              </ElCheckbox>
            </ElFormItem>

            <ElFormItem
              v-if="alreadyPaid"
              :label="$t('editInvoice.generalInformation.datePaid.label')"
              prop="datePaid"
            >
              <!-- todo #78: format from cldr https://github.com/ElemeFE/element/issues/11353 -->
              <ElDatePicker
                v-model="invoice.datePaid"
                type="date"
                :placeholder="$t('editInvoice.generalInformation.datePaid.placeholder')"
                value-format="yyyy-MM-dd"
              />
            </ElFormItem>
          </div>

          <div class="col col-xs-12 col-lg-6">
            <h2>{{ $t('editInvoice.additionalNotes.header') }}</h2>

            <ElFormItem
              :label="$t('editInvoice.additionalNotes.notes.label')"
              prop="notes"
            >
              <SaNotesInput
                v-model="invoice.notes"
                :placeholder="$t('editInvoice.additionalNotes.notes.placeholder')"
              />
            </ElFormItem>

            <h2>{{ $t('editInvoice.attachments.header') }}</h2>

            <ElFormItem>
              <SaDocumentsUpload
                ref="documentsUpload"
                :documents-ids="invoice.attachments"
                @uploads-completed="onDocumentsUploadSuccess"
                @uploads-failed="onDocumentsUploadFailure"
              />
            </ElFormItem>
          </div>
        </div>
      </template>

      <template #buttons-bar>
        <ElButton @click="navigateToInvoicesOverview">
          {{ $t('editInvoice.cancel') }}
        </ElButton>
        <ElButton
          type="primary"
          @click="save"
        >
          {{ $t('editInvoice.save') }}
        </ElButton>
      </template>
    </SaForm>
  </div>
</template>

<script>
  import { api } from '@/services/api';
  import MoneyInput from '@/components/MoneyInput';
  import SaCurrencyInput from '@/components/SaCurrencyInput';
  import SaDocumentsUpload from '@/components/documents/SaDocumentsUpload';
  import SaNotesInput from '@/components/SaNotesInput';
  import SaForm from '@/components/SaForm';
  import withGeneralTaxes from '@/components/mixins/with-general-taxes';
  import withWorkspaces from '@/components/mixins/with-workspaces';
  import SaCustomerInput from '@/components/customer/SaCustomerInput';

  export default {
    name: 'EditInvoice',

    components: {
      SaCustomerInput,
      SaCurrencyInput,
      SaForm,
      SaNotesInput,
      SaDocumentsUpload,
      MoneyInput,
    },

    mixins: [withGeneralTaxes, withWorkspaces],

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
          generalTax: null,
        },
        invoiceValidationRules: {
          customer: {
            required: true,
            message: this.$t('editInvoice.validations.customer'),
          },
          currency: {
            required: true,
            message: this.$t('editInvoice.validations.currency'),
          },
          title: {
            required: true,
            message: this.$t('editInvoice.validations.title'),
          },
          amount: {
            required: true,
            message: this.$t('editInvoice.validations.amount'),
          },
          dateIssued: {
            required: true,
            message: this.$t('editInvoice.validations.dateIssued'),
          },
          dueDate: {
            required: true,
            message: this.$t('editInvoice.validations.dueDate'),
          },
          dateSent: {
            required: true,
            message: this.$t('editInvoice.validations.dateSent'),
          },
          datePaid: {
            required: true,
            message: this.$t('editInvoice.validations.datePaid'),
          },
        },
        alreadySent: false,
        alreadyPaid: false,
      };
    },

    computed: {
      pageHeader() {
        return this.isEditing
          ? this.$t('editInvoice.pageHeader.edit') : this.$t('editInvoice.pageHeader.create');
      },

      isEditing() {
        return this.$route.params.id != null;
      },
    },

    async created() {
      if (this.isEditing) {
        const invoiceResponse = await api
          .get(`/workspaces/${this.currentWorkspace.id}/invoices/${this.$route.params.id}`);
        this.invoice = { ...invoiceResponse.data };
        this.alreadyPaid = this.invoice.datePaid != null;
        this.alreadySent = this.invoice.dateSent != null;
      }
    },

    methods: {
      navigateToInvoicesOverview() {
        this.$router.push({ name: 'invoices-overview' });
      },

      async onDocumentsUploadSuccess(documentsIds) {
        this.pushInvoice(documentsIds);
      },

      async onDocumentsUploadFailure() {
        this.$message({
          showClose: true,
          message: this.$t('editInvoice.documentsUploadFailure'),
          type: 'error',
        });
      },

      async save() {
        try {
          await this.$refs.invoiceForm.validate();
        } catch (e) {
          return;
        }

        await this.$refs.documentsUpload.submitUploads();
      },

      async cancelInvoice() {
        try {
          await this.$confirm(
            this.$t('editInvoice.cancelInvoice.confirm.message'),
            'Warning', {
              confirmButtonText: this.$t('editInvoice.cancelInvoice.confirm.yes'),
              cancelButtonText: this.$t('editInvoice.cancelInvoice.confirm.no'),
              type: 'warning',
            },
          );
        } catch (e) {
          return;
        }

        this.invoice.dateCancelled = api.dateToString(new Date());
        this.pushInvoice(this.invoice.attachments);
      },

      async pushInvoice(attachments) {
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
          attachments,
          notes: this.invoice.notes,
          generalTax: this.invoice.generalTax,
        };

        if (this.isEditing) {
          await api.put(`/workspaces/${this.currentWorkspace.id}/invoices/${this.invoice.id}`, invoiceToPush);
        } else {
          await api.post(`/workspaces/${this.currentWorkspace.id}/invoices`, invoiceToPush);
        }

        await this.$router.push({ name: 'invoices-overview' });
      },
    },
  };
</script>
