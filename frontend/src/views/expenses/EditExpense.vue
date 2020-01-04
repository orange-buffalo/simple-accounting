<template>
  <div>
    <div class="sa-page-header">
      <h1>{{ pageHeader }}</h1>
    </div>

    <SaForm
      ref="expenseForm"
      :model="expense"
      :rules="expenseValidationRules"
    >
      <template #default>
        <div class="row">
          <div class="col col-xs-12 col-lg-6">
            <h2>{{ $t('editExpense.generalInformation.header') }}</h2>

            <ElFormItem
              :label="$t('editExpense.generalInformation.category.label')"
              prop="category"
            >
              <ElSelect
                v-model="expense.category"
                :placeholder="$t('editExpense.generalInformation.category.placeholder')"
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
              :label="$t('editExpense.generalInformation.title.label')"
              prop="title"
            >
              <ElInput
                v-model="expense.title"
                :placeholder="$t('editExpense.generalInformation.title.placeholder')"
              />
            </ElFormItem>

            <ElFormItem
              :label="$t('editExpense.generalInformation.currency.label')"
              prop="currency"
            >
              <SaCurrencyInput v-model="expense.currency" />
            </ElFormItem>

            <ElFormItem
              :label="$t('editExpense.generalInformation.originalAmount.label')"
              prop="originalAmount"
            >
              <MoneyInput
                v-model="expense.originalAmount"
                :currency="expense.currency"
              />
            </ElFormItem>

            <ElFormItem
              :label="$t('editExpense.generalInformation.datePaid.label')"
              prop="datePaid"
            >
              <!-- todo #78: format from cldr https://github.com/ElemeFE/element/issues/11353 -->
              <ElDatePicker
                v-model="expense.datePaid"
                type="date"
                :placeholder="$t('editExpense.generalInformation.datePaid.placeholder')"
                value-format="yyyy-MM-dd"
              />
            </ElFormItem>

            <ElFormItem
              v-if="isInForeignCurrency"
              :label="$t('editExpense.generalInformation.convertedAmountInDefaultCurrency.label', [defaultCurrency])"
              prop="convertedAmountInDefaultCurrency"
            >
              <MoneyInput
                v-model="expense.convertedAmountInDefaultCurrency"
                :currency="defaultCurrency"
              />
            </ElFormItem>

            <ElFormItem v-if="isInForeignCurrency">
              <ElCheckbox v-model="expense.useDifferentExchangeRateForIncomeTaxPurposes">
                {{ $t('editExpense.generalInformation.useDifferentExchangeRateForIncomeTaxPurposes.label') }}
              </ElCheckbox>
            </ElFormItem>

            <!-- eslint-disable max-len-->
            <ElFormItem
              v-if="expense.useDifferentExchangeRateForIncomeTaxPurposes"
              :label="$t('editExpense.generalInformation.incomeTaxableAmountInDefaultCurrency.label', [defaultCurrency])"
              prop="incomeTaxableAmountInDefaultCurrency"
            >
              <MoneyInput
                v-model="expense.incomeTaxableAmountInDefaultCurrency"
                :currency="defaultCurrency"
              />
            </ElFormItem>

            <ElFormItem
              :label="$t('editExpense.generalInformation.generalTax.label')"
              prop="generalTax"
            >
              <ElSelect
                v-model="expense.generalTax"
                clearable
                :placeholder="$t('editExpense.generalInformation.generalTax.placeholder')"
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
              <ElCheckbox v-model="partialForBusiness">
                {{ $t('editExpense.generalInformation.partialForBusiness.label') }}
              </ElCheckbox>
            </ElFormItem>

            <ElFormItem
              v-if="partialForBusiness"
              :label="$t('editExpense.generalInformation.percentOnBusiness.label')"
              prop="percentOnBusiness"
            >
              <ElInputNumber
                v-model="expense.percentOnBusiness"
                :min="0"
                :max="100"
              />
            </ElFormItem>
          </div>

          <div class="col col-xs-12 col-lg-6">
            <h2>{{ $t('editExpense.additionalInformation.header') }}</h2>

            <ElFormItem
              :label="$t('editExpense.additionalInformation.notes.label')"
              prop="notes"
            >
              <SaNotesInput
                v-model="expense.notes"
                :placeholder="$t('editExpense.additionalInformation.notes.placeholder')"
              />
            </ElFormItem>

            <h2>{{ $t('editExpense.attachments.header') }}</h2>

            <ElFormItem>
              <SaDocumentsUpload
                ref="documentsUpload"
                :documents-ids="expense.attachments"
                @uploads-completed="onDocumentsUploadSuccess"
                @uploads-failed="onDocumentsUploadFailure"
              />
            </ElFormItem>
          </div>
        </div>
      </template>

      <template #buttons-bar>
        <ElButton @click="navigateToExpensesOverview">
          {{ $t('editExpense.cancel') }}
        </ElButton>
        <ElButton
          type="primary"
          @click="save"
        >
          {{ $t('editExpense.save') }}
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
  import withCategories from '@/components/mixins/with-categories';
  import withGeneralTaxes from '@/components/mixins/with-general-taxes';
  import withWorkspaces from '@/components/mixins/with-workspaces';

  export default {
    name: 'EditExpense',

    components: {
      SaCurrencyInput,
      SaForm,
      SaNotesInput,
      SaDocumentsUpload,
      MoneyInput,
    },

    mixins: [withGeneralTaxes, withCategories, withWorkspaces],

    props: {
      id: {
        type: String,
        default: null,
      },
      prototype: {
        type: Object,
        default: null,
      },
    },

    data() {
      return {
        expense: {
          category: null,
          title: null,
          currency: null,
          originalAmount: null,
          convertedAmountInDefaultCurrency: null,
          incomeTaxableAmountInDefaultCurrency: null,
          useDifferentExchangeRateForIncomeTaxPurposes: false,
          attachments: [],
          percentOnBusiness: 100,
          notes: null,
          datePaid: new Date(),
          generalTax: null,
          id: this.id,
        },
        expenseValidationRules: {
          currency: {
            required: true,
            message: this.$t('editExpense.validations.currency'),
          },
          title: {
            required: true,
            message: this.$t('editExpense.validations.title'),
          },
          datePaid: {
            required: true,
            message: this.$t('editExpense.validations.datePaid'),
          },
          originalAmount: {
            required: true,
            message: this.$t('editExpense.validations.originalAmount'),
          },
        },
        partialForBusiness: false,
      };
    },

    computed: {
      isInForeignCurrency() {
        return this.expense.currency !== this.defaultCurrency;
      },

      pageHeader() {
        return this.expense.id ? this.$t('editExpense.pageHeader.edit') : this.$t('editExpense.pageHeader.create');
      },
    },

    async created() {
      if (this.id) {
        await this.loadExpense();
      } else if (this.prototype) {
        this.copyExpenseProperties(this.prototype, {
          datePaid: null,
          id: null,
        });
        await this.setupComponentState();
      }
    },

    methods: {
      async loadExpense() {
        const expenseResponse = await api.get(`/workspaces/${this.currentWorkspace.id}/expenses/${this.id}`);
        this.copyExpenseProperties(expenseResponse.data);
        await this.setupComponentState();
      },

      copyExpenseProperties(sourceExpense, overrides) {
        const {
          convertedAmounts,
          incomeTaxableAmounts,
          generalTaxRateInBps,
          generalTaxAmount,
          status,
          version,
          timeRecorded,
          ...expenseEditProperties
        } = sourceExpense;
        this.expense = {
          ...expenseEditProperties,
          convertedAmountInDefaultCurrency: convertedAmounts.originalAmountInDefaultCurrency,
          incomeTaxableAmountInDefaultCurrency: incomeTaxableAmounts.originalAmountInDefaultCurrency,
          ...overrides,
        };
      },

      async setupComponentState() {
        this.partialForBusiness = this.expense.percentOnBusiness !== 100;
      },

      navigateToExpensesOverview() {
        this.$router.push({ name: 'expenses-overview' });
      },

      async onDocumentsUploadSuccess(documentsIds) {
        const {
          id,
          ...expensePropertiesToPush
        } = this.expense;

        const expenseToPush = {
          ...expensePropertiesToPush,
          percentOnBusiness: this.partialForBusiness ? this.expense.percentOnBusiness : null,
          attachments: documentsIds,
        };

        if (this.expense.id) {
          await api.put(`/workspaces/${this.currentWorkspace.id}/expenses/${this.expense.id}`, expenseToPush);
        } else {
          await api.post(`/workspaces/${this.currentWorkspace.id}/expenses`, expenseToPush);
        }

        await this.$router.push({ name: 'expenses-overview' });
      },

      async onDocumentsUploadFailure() {
        this.$message({
          showClose: true,
          message: this.$t('editExpense.documentsUploadFailure'),
          type: 'error',
        });
      },

      async save() {
        try {
          await this.$refs.expenseForm.validate();
        } catch (e) {
          return;
        }

        this.$refs.documentsUpload.submitUploads();
      },
    },
  };
</script>
