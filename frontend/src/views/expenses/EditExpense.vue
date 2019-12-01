<template>
  <div>
    <div class="sa-page-header">
      <h1>{{ pageHeader }}</h1>
    </div>

    <div class="sa-form">
      <ElForm
        ref="expenseForm"
        :model="expense"
        label-position="right"
        label-width="200px"
        :rules="expenseValidationRules"
      >
        <div class="row">
          <div class="col col-xs-12 col-lg-6">
            <h2>General Information</h2>

            <ElFormItem
              label="Category"
              prop="category"
            >
              <ElSelect
                v-model="expense.category"
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
                v-model="expense.title"
                placeholder="Provide a short summary"
              />
            </ElFormItem>

            <ElFormItem
              label="Currency"
              prop="currency"
            >
              <CurrencyInput v-model="expense.currency" />
            </ElFormItem>

            <ElFormItem
              label="Original Amount"
              prop="originalAmount"
            >
              <MoneyInput
                v-model="expense.originalAmount"
                :currency="expense.currency"
              />
            </ElFormItem>

            <ElFormItem
              label="Date Paid"
              prop="datePaid"
            >
              <!-- todo #78: format from cldr https://github.com/ElemeFE/element/issues/11353 -->
              <ElDatePicker
                v-model="expense.datePaid"
                type="date"
                placeholder="Date expense is paid"
                value-format="yyyy-MM-dd"
              />
            </ElFormItem>

            <ElFormItem
              v-if="isInForeignCurrency"
              :label="`Amount in ${defaultCurrency}`"
              prop="convertedAmountInDefaultCurrency"
            >
              <MoneyInput
                v-model="expense.convertedAmountInDefaultCurrency"
                :currency="defaultCurrency"
              />
            </ElFormItem>

            <ElFormItem v-if="isInForeignCurrency">
              <ElCheckbox v-model="expense.useDifferentExchangeRateForIncomeTaxPurposes">
                Using different exchange rate for taxation purposes
              </ElCheckbox>
            </ElFormItem>

            <ElFormItem
              v-if="expense.useDifferentExchangeRateForIncomeTaxPurposes"
              :label="`Amount in ${defaultCurrency} for taxation purposes`"
              prop="incomeTaxableAmountInDefaultCurrency"
            >
              <MoneyInput
                v-model="expense.incomeTaxableAmountInDefaultCurrency"
                :currency="defaultCurrency"
              />
            </ElFormItem>

            <ElFormItem
              label="Included General Tax"
              prop="generalTax"
            >
              <ElSelect
                v-model="expense.generalTax"
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
              <ElCheckbox v-model="partialForBusiness">
                Partial Business Purpose
              </ElCheckbox>
            </ElFormItem>

            <ElFormItem
              v-if="partialForBusiness"
              label="% related to business activities"
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
            <h2>Additional notes</h2>

            <ElFormItem
              label="Notes"
              prop="notes"
            >
              <ElInput
                v-model="expense.notes"
                type="textarea"
                placeholder="Any additional information to be stored for this expense record"
                rows="5"
              />
            </ElFormItem>

            <SaMarkdownOutput
              v-if="expense.notes"
              :source="expense.notes"
              preview
            />

            <h2>Attachments</h2>

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

        <hr>

        <div class="sa-buttons-bar">
          <ElButton @click="navigateToExpensesOverview">
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
  import SaMarkdownOutput from '@/components/SaMarkdownOutput';
  import withWorkspaces from '@/components/mixins/with-workspaces';
  import SaDocumentsUpload from '@/components/documents/SaDocumentsUpload';

  export default {
    name: 'EditExpense',

    components: {
      SaDocumentsUpload,
      CurrencyInput,
      MoneyInput,
      SaMarkdownOutput,
    },

    mixins: [withMediumDateFormatter, withGeneralTaxes, withCategories, withWorkspaces],

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
            message: 'Please select a currency',
          },
          title: {
            required: true,
            message: 'Please provide the title',
          },
          datePaid: {
            required: true,
            message: 'Please provide the date when expense is paid',
          },
          originalAmount: {
            required: true,
            message: 'Please provide expense amount',
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
        return this.expense.id ? 'Edit Expense' : 'Record New Expense';
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
          message: 'Some of the documents have not been uploaded. Please retry or remove them.',
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
