<template>
  <div>
    <div class="sa-page-header">
      <h1>{{ pageHeader }}</h1>
    </div>

    <SaForm
      ref="incomeForm"
      :model="income"
      :rules="incomeValidationRules"
    >
      <template #default>
        <div class="row">
          <div class="col col-xs-12 col-lg-6">
            <h2>{{ $t('editIncome.generalInformation.header') }}</h2>

            <ElFormItem
              :label="$t('editIncome.generalInformation.category.label')"
              prop="category"
            >
              <SaCategoryInput
                v-model="income.category"
                :placeholder="$t('editIncome.generalInformation.category.placeholder')"
              />
            </ElFormItem>

            <ElFormItem
              :label="$t('editIncome.generalInformation.title.label')"
              prop="title"
            >
              <ElInput
                v-model="income.title"
                :placeholder="$t('editIncome.generalInformation.title.placeholder')"
              />
            </ElFormItem>

            <ElFormItem
              :label="$t('editIncome.generalInformation.currency.label')"
              prop="currency"
            >
              <SaCurrencyInput v-model="income.currency" />
            </ElFormItem>

            <ElFormItem
              :label="$t('editIncome.generalInformation.originalAmount.label')"
              prop="originalAmount"
            >
              <MoneyInput
                v-model="income.originalAmount"
                :currency="income.currency"
              />
            </ElFormItem>

            <ElFormItem
              :label="$t('editIncome.generalInformation.dateReceived.label')"
              prop="dateReceived"
            >
              <!-- todo #78: format from cldr https://github.com/ElemeFE/element/issues/11353 -->
              <ElDatePicker
                v-model="income.dateReceived"
                type="date"
                :placeholder="$t('editIncome.generalInformation.dateReceived.placeholder')"
                value-format="yyyy-MM-dd"
              />
            </ElFormItem>

            <ElFormItem
              v-if="isInForeignCurrency"
              :label="$t('editIncome.generalInformation.convertedAmountInDefaultCurrency.label', [defaultCurrency])"
              prop="convertedAmountInDefaultCurrency"
            >
              <MoneyInput
                v-model="income.convertedAmountInDefaultCurrency"
                :currency="defaultCurrency"
              />
            </ElFormItem>

            <ElFormItem v-if="isInForeignCurrency">
              <ElCheckbox v-model="income.useDifferentExchangeRateForIncomeTaxPurposes">
                {{ $t('editIncome.generalInformation.useDifferentExchangeRateForIncomeTaxPurposes.label') }}
              </ElCheckbox>
            </ElFormItem>

            <ElFormItem
              v-if="income.useDifferentExchangeRateForIncomeTaxPurposes"
              :label="$t('editIncome.generalInformation.incomeTaxableAmountInDefaultCurrency.label', [defaultCurrency])"
              prop="incomeTaxableAmountInDefaultCurrency"
            >
              <MoneyInput
                v-model="income.incomeTaxableAmountInDefaultCurrency"
                :currency="defaultCurrency"
              />
            </ElFormItem>

            <ElFormItem
              :label="$t('editIncome.generalInformation.generalTax.label')"
              prop="generalTax"
            >
              <SaGeneralTaxInput
                v-model="income.generalTax"
                clearable
                :placeholder="$t('editIncome.generalInformation.generalTax.placeholder')"
              />
            </ElFormItem>
          </div>

          <div class="col col-xs-12 col-lg-6">
            <h2>{{ $t('editIncome.additionalInformation.header') }}</h2>

            <ElFormItem
              v-if="income.linkedInvoice"
              :label="$t('editIncome.additionalInformation.linkedInvoice.label')"
              prop="reportedAmountInDefaultCurrency"
            >
              <span>{{ income.linkedInvoice.title }}</span>
            </ElFormItem>

            <ElFormItem
              :label="$t('editIncome.additionalInformation.notes.label')"
              prop="notes"
            >
              <SaNotesInput
                v-model="income.notes"
                :placeholder="$t('editIncome.additionalInformation.notes.placeholder')"
              />
            </ElFormItem>

            <h2>{{ $t('editIncome.attachments.header') }}</h2>

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
      </template>

      <template #buttons-bar>
        <ElButton @click="navigateToIncomesOverview">
          {{ $t('editIncome.cancel') }}
        </ElButton>
        <ElButton
          type="primary"
          @click="save"
        >
          {{ $t('editIncome.save') }}
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
  import withWorkspaces from '@/components/mixins/with-workspaces';
  import SaCategoryInput from '@/components/category/SaCategoryInput';
  import SaGeneralTaxInput from '@/components/general-tax/SaGeneralTaxInput';

  export default {
    name: 'EditIncome',

    components: {
      SaGeneralTaxInput,
      SaCategoryInput,
      SaCurrencyInput,
      SaForm,
      SaNotesInput,
      SaDocumentsUpload,
      MoneyInput,
    },

    mixins: [withWorkspaces],

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
            message: this.$t('editIncome.validations.currency'),
          },
          title: {
            required: true,
            message: this.$t('editIncome.validations.title'),
          },
          dateReceived: {
            required: true,
            message: this.$t('editIncome.validations.dateReceived'),
          },
          originalAmount: {
            required: true,
            message: this.$t('editIncome.validations.originalAmount'),
          },
        },
      };
    },

    computed: {
      isInForeignCurrency() {
        return this.income.currency !== this.defaultCurrency;
      },

      pageHeader() {
        return this.$route.params.id
          ? this.$t('editIncome.pageHeader.edit') : this.$t('editIncome.pageHeader.create');
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
          message: this.$t('editIncome.documentsUploadFailure'),
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
