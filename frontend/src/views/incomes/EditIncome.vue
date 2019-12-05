<template>
  <div>
    <div class="sa-page-header">
      <h1>{{ pageHeader }}</h1>
    </div>

    <div class="sa-form">
      <ElForm
        ref="incomeForm"
        :model="income"
        label-position="right"
        :rules="incomeValidationRules"
      >
        <div class="row">
          <div class="col col-xs-12 col-lg-6">
            <h2>General Information</h2>

            <ElFormItem
              label="Category"
              prop="category"
            >
              <ElSelect
                v-model="income.category"
                placeholder="Select a category"
              >
                <ElOption
                  v-for="category in categories"
                  :key="category.id"
                  :label="category.name"
                  :value="category.id"
                />
              </ElSelect>
            </ElFormItem>

            <ElFormItem
              label="Description / Title"
              prop="title"
            >
              <ElInput
                v-model="income.title"
                placeholder="Provide a short summary"
              />
            </ElFormItem>

            <ElFormItem
              label="Currency"
              prop="currency"
            >
              <CurrencyInput v-model="income.currency" />
            </ElFormItem>

            <ElFormItem
              label="Amount"
              prop="originalAmount"
            >
              <MoneyInput
                v-model="income.originalAmount"
                :currency="income.currency"
              />
            </ElFormItem>

            <ElFormItem
              label="Date Received"
              prop="dateReceived"
            >
              <!-- todo #78: format from cldr https://github.com/ElemeFE/element/issues/11353 -->
              <ElDatePicker
                v-model="income.dateReceived"
                type="date"
                placeholder="Date income is received"
                value-format="yyyy-MM-dd"
              />
            </ElFormItem>

            <ElFormItem
              v-if="isInForeignCurrency"
              :label="`Amount in ${defaultCurrency}`"
              prop="convertedAmountInDefaultCurrency"
            >
              <MoneyInput
                v-model="income.convertedAmountInDefaultCurrency"
                :currency="defaultCurrency"
              />
            </ElFormItem>

            <ElFormItem v-if="isInForeignCurrency">
              <ElCheckbox v-model="income.useDifferentExchangeRateForIncomeTaxPurposes">
                Using different exchange rate for taxation purposes
              </ElCheckbox>
            </ElFormItem>

            <ElFormItem
              v-if="income.useDifferentExchangeRateForIncomeTaxPurposes"
              :label="`Amount in ${defaultCurrency} for taxation purposes`"
              prop="incomeTaxableAmountInDefaultCurrency"
            >
              <MoneyInput
                v-model="income.incomeTaxableAmountInDefaultCurrency"
                :currency="defaultCurrency"
              />
            </ElFormItem>

            <ElFormItem
              label="Included General Tax"
              prop="generalTax"
            >
              <ElSelect
                v-model="income.generalTax"
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
          </div>

          <div class="col col-xs-12 col-lg-6">
            <h2>Additional Information</h2>

            <ElFormItem
              v-if="income.linkedInvoice"
              label="Linked Invoice"
              prop="reportedAmountInDefaultCurrency"
            >
              <span>{{ income.linkedInvoice.title }}</span>
            </ElFormItem>

            <ElFormItem
              label="Notes"
              prop="notes"
            >
              <SaNotesInput
                v-model="income.notes"
                placeholder="Any additional information to be stored for this income record"
              />
            </ElFormItem>

            <h2>Attachments</h2>

            <ElFormItem>
              <SaDocumentsUpload
                ref="documentsUpload"
                :documents-ids="income.attachments"
                @uploads-completed="onDocumentsUploadSuccess"
                @uploads-failed="onDocumentsUploadFailure"
              />
            </ElFormItem>
          </div>
        </div>
        <hr>

        <div class="sa-buttons-bar">
          <ElButton @click="navigateToIncomesOverview">
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
  import { api } from '@/services/api';
  import CurrencyInput from '@/components/CurrencyInput';
  import MoneyInput from '@/components/MoneyInput';
  import withMediumDateFormatter from '@/components/mixins/with-medium-date-formatter';
  import withGeneralTaxes from '@/components/mixins/with-general-taxes';
  import withCategories from '@/components/mixins/with-categories';
  import withWorkspaces from '@/components/mixins/with-workspaces';
  import SaDocumentsUpload from '@/components/documents/SaDocumentsUpload';
  import SaNotesInput from '@/components/SaNotesInput';

  export default {
    name: 'EditIncome',

    components: {
      SaNotesInput,
      SaDocumentsUpload,
      CurrencyInput,
      MoneyInput,
    },

    mixins: [withMediumDateFormatter, withGeneralTaxes, withCategories, withWorkspaces],

    data() {
      return {
        income: {
          category: null,
          title: null,
          currency: null,
          originalAmount: null,
          convertedAmountInDefaultCurrency: null,
          incomeTaxableAmountInDefaultCurrency: null,
          useDifferentExchangeRateForIncomeTaxPurposes: false,
          attachments: [],
          notes: null,
          dateReceived: new Date(),
          generalTax: null,
        },
        incomeValidationRules: {
          currency: {
            required: true,
            message: 'Please select a currency',
          },
          title: {
            required: true,
            message: 'Please provide the title',
          },
          dateReceived: {
            required: true,
            message: 'Please provide the date when income is received',
          },
          originalAmount: {
            required: true,
            message: 'Please provide income amount',
          },
        },
      };
    },

    computed: {
      isInForeignCurrency() {
        return this.income.currency !== this.defaultCurrency;
      },

      pageHeader() {
        return this.$route.params.id ? 'Edit Income' : 'Record New Income';
      },
    },

    async created() {
      if (this.$route.params.id) {
        const incomeResponse = await api
          .get(`/workspaces/${this.currentWorkspace.id}/incomes/${this.$route.params.id}`);

        const {
          convertedAmounts,
          incomeTaxableAmounts,
          generalTaxRateInBps,
          generalTaxAmount,
          status,
          version,
          timeRecorded,
          ...incomeEditProperties
        } = incomeResponse.data;
        this.income = {
          ...incomeEditProperties,
          convertedAmountInDefaultCurrency: convertedAmounts.originalAmountInDefaultCurrency,
          incomeTaxableAmountInDefaultCurrency: incomeTaxableAmounts.originalAmountInDefaultCurrency,
        };
      }
    },

    methods: {
      navigateToIncomesOverview() {
        this.$router.push({ name: 'incomes-overview' });
      },

      async onDocumentsUploadSuccess(documentsIds) {
        const {
          id,
          ...incomePropertiesToPush
        } = this.income;

        const incomeToPush = {
          ...incomePropertiesToPush,
          attachments: documentsIds,
        };

        if (this.income.id) {
          await api.put(`/workspaces/${this.currentWorkspace.id}/incomes/${this.income.id}`, incomeToPush);
        } else {
          await api.post(`/workspaces/${this.currentWorkspace.id}/incomes`, incomeToPush);
        }
        await this.$router.push({ name: 'incomes-overview' });
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
          await this.$refs.incomeForm.validate();
        } catch (e) {
          return;
        }

        await this.$refs.documentsUpload.submitUploads();
      },
    },
  };
</script>
