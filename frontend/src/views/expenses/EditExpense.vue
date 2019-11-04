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
              label="Amount"
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

            <ElFormItem v-if="!isInDefaultCurrency">
              <ElCheckbox v-model="alreadyConverted">
                Already converted
              </ElCheckbox>
            </ElFormItem>

            <ElFormItem
              v-if="defaultCurrencyAmountVisible"
              :label="`Amount in ${defaultCurrency}`"
              prop="amountInDefaultCurrency"
            >
              <MoneyInput
                v-model="expense.amountInDefaultCurrency"
                :currency="defaultCurrency"
              />
            </ElFormItem>

            <ElFormItem v-if="alreadyConverted">
              <ElCheckbox v-model="reportedAnotherExchangeRate">
                Reported converted amount is different (using another rate)
              </ElCheckbox>
            </ElFormItem>

            <ElFormItem
              v-if="actualAmountVisible"
              label="Reported Amount"
              prop="actualAmountInDefaultCurrency"
            >
              <MoneyInput
                v-model="expense.actualAmountInDefaultCurrency"
                :currency="defaultCurrency"
              />
            </ElFormItem>

            <ElFormItem>
              <ElCheckbox v-model="partialForBusiness">
                Expense is partially purposed for the business needs
              </ElCheckbox>
            </ElFormItem>

            <ElFormItem
              v-if="percentOnBusinessVisible"
              label="% spent on business"
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

            <DocumentsUpload
              ref="documentsUpload"
              v-model="expense.uploads"
              form-property="uploads"
            />
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
  import { assign, isNil } from 'lodash';
  import { api } from '@/services/api';
  import DocumentsUpload from '@/components/DocumentsUpload';
  import CurrencyInput from '@/components/CurrencyInput';
  import MoneyInput from '@/components/MoneyInput';
  import { UploadsInfo } from '@/components/uploads-info';
  import withMediumDateFormatter from '@/components/mixins/with-medium-date-formatter';
  import withGeneralTaxes from '@/components/mixins/with-general-taxes';
  import withCategories from '@/components/mixins/with-categories';
  import SaMarkdownOutput from '@/components/SaMarkdownOutput';
  import withWorkspaces from '@/components/mixins/with-workspaces';

  export default {
    name: 'EditExpense',

    components: {
      DocumentsUpload,
      CurrencyInput,
      MoneyInput,
      SaMarkdownOutput,
    },

    mixins: [withMediumDateFormatter, withGeneralTaxes, withCategories, withWorkspaces],

    props: {
      id: {
        type: Number,
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
          amountInDefaultCurrency: null,
          actualAmountInDefaultCurrency: null,
          attachments: [],
          percentOnBusiness: 100,
          notes: null,
          datePaid: new Date(),
          uploads: new UploadsInfo(),
          generalTax: null,
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
        alreadyConverted: false,
        reportedAnotherExchangeRate: false,
        partialForBusiness: false,
      };
    },

    computed: {
      isInDefaultCurrency() {
        return this.expense.currency === this.defaultCurrency;
      },

      defaultCurrencyAmountVisible() {
        return this.alreadyConverted && !this.isInDefaultCurrency;
      },

      actualAmountVisible() {
        return this.defaultCurrencyAmountVisible && this.reportedAnotherExchangeRate && !this.isInDefaultCurrency;
      },

      percentOnBusinessVisible() {
        return this.partialForBusiness;
      },

      pageHeader() {
        return this.id ? 'Edit Expense' : 'Record New Expense';
      },
    },

    async created() {
      if (this.id) {
        await this.loadExpense();
      } else if (this.prototype) {
        await this.copyExpenseFromPrototype();
      }
    },

    methods: {
      async loadExpense() {
        const expenseResponse = await api.get(`/workspaces/${this.currentWorkspace.id}/expenses/${this.id}`);
        this.expense = assign({}, this.expense, expenseResponse.data);
        await this.setupComponentState();
      },

      async copyExpenseFromPrototype() {
        this.expense = {
          ...this.expense,
          ...{
            category: this.prototype.category,
            title: this.prototype.title,
            currency: this.prototype.currency,
            originalAmount: this.prototype.originalAmount,
            amountInDefaultCurrency: this.prototype.amountInDefaultCurrency,
            actualAmountInDefaultCurrency: this.prototype.actualAmountInDefaultCurrency,
            percentOnBusiness: this.prototype.percentOnBusiness,
            notes: this.prototype.notes,
            generalTax: this.prototype.generalTax,
          },
        };
        await this.setupComponentState();
      },

      async setupComponentState() {
        this.alreadyConverted = this.expense.currency !== this.defaultCurrency
          && !isNil(this.expense.amountInDefaultCurrency)
          && this.expense.amountInDefaultCurrency > 0;

        this.reportedAnotherExchangeRate = this.expense.currency !== this.defaultCurrency
          && !isNil(this.expense.actualAmountInDefaultCurrency)
          && (this.expense.actualAmountInDefaultCurrency !== this.expense.amountInDefaultCurrency);

        this.partialForBusiness = this.expense.percentOnBusiness !== 100;

        if (this.expense.attachments && this.expense.attachments.length) {
          const attachments = await api.pageRequest(`/workspaces/${this.currentWorkspace.id}/documents`)
            .eager()
            .eqFilter('id', this.expense.attachments)
            .getPageData();
          attachments.forEach(attachment => this.expense.uploads.add(attachment));
        }
      },

      navigateToExpensesOverview() {
        this.$router.push({ name: 'expenses-overview' });
      },

      async save() {
        try {
          await this.$refs.expenseForm.validate();
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

        const expenseToPush = {
          category: this.expense.category,
          datePaid: this.expense.datePaid,
          title: this.expense.title,
          currency: this.expense.currency,
          originalAmount: this.expense.originalAmount,
          amountInDefaultCurrency: this.alreadyConverted ? this.expense.amountInDefaultCurrency : null,
          actualAmountInDefaultCurrency: this.reportedAnotherExchangeRate
            ? this.expense.actualAmountInDefaultCurrency : this.expense.amountInDefaultCurrency,
          attachments: this.expense.uploads.getDocumentsIds(),
          percentOnBusiness: this.partialForBusiness ? this.expense.percentOnBusiness : null,
          notes: this.expense.notes,
          generalTax: this.expense.generalTax,
        };

        if (this.expense.id) {
          await api.put(`/workspaces/${this.currentWorkspace.id}/expenses/${this.expense.id}`, expenseToPush);
        } else {
          await api.post(`/workspaces/${this.currentWorkspace.id}/expenses`, expenseToPush);
        }
        await this.$router.push({ name: 'expenses-overview' });
      },
    },
  };
</script>
