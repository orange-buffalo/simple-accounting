<template>
  <div>
    <div class="sa-page-header">
      <h1>{{ pageHeader }}</h1>
    </div>

    <div class="sa-form">
      <el-form
        ref="expenseForm"
        :model="expense"
        label-position="right"
        label-width="200px"
        :rules="expenseValidationRules"
      >
        <div class="row">
          <div class="col col-xs-12 col-lg-6">
            <h2>General Information</h2>

            <el-form-item
              label="Category"
              prop="category"
            >
              <el-select
                v-model="expense.category"
                placeholder="Select a category"
              >
                <el-option
                  v-for="category in categories"
                  :key="category.id"
                  :label="category.name"
                  :value="category.id"
                />
              </el-select>
            </el-form-item>

            <el-form-item
              label="Description / Title"
              prop="title"
            >
              <el-input
                v-model="expense.title"
                placeholder="Provide a short summary"
              />
            </el-form-item>

            <el-form-item
              label="Currency"
              prop="currency"
            >
              <currency-input v-model="expense.currency" />
            </el-form-item>

            <el-form-item
              label="Amount"
              prop="originalAmount"
            >
              <money-input
                v-model="expense.originalAmount"
                :currency="expense.currency"
              />
            </el-form-item>

            <el-form-item
              label="Date Paid"
              prop="datePaid"
            >
              <!-- todo #78: format from cldr https://github.com/ElemeFE/element/issues/11353 -->
              <el-date-picker
                v-model="expense.datePaid"
                type="date"
                placeholder="Date expense is paid"
                value-format="yyyy-MM-dd"
              />
            </el-form-item>

            <el-form-item
              label="Included General Tax"
              prop="generalTax"
            >
              <el-select
                v-model="expense.generalTax"
                clearable
                placeholder="Select a tax"
              >
                <el-option
                  v-for="tax in generalTaxes"
                  :key="tax.id"
                  :label="tax.title"
                  :value="tax.id"
                />
              </el-select>
            </el-form-item>

            <el-form-item v-if="!isInDefaultCurrency">
              <el-checkbox v-model="alreadyConverted">
                Already converted
              </el-checkbox>
            </el-form-item>

            <el-form-item
              v-if="defaultCurrencyAmountVisible"
              :label="`Amount in ${defaultCurrency}`"
              prop="amountInDefaultCurrency"
            >
              <money-input
                v-model="expense.amountInDefaultCurrency"
                :currency="defaultCurrency"
              />
            </el-form-item>

            <el-form-item v-if="alreadyConverted">
              <el-checkbox v-model="reportedAnotherExchangeRate">
                Reported converted amount is different (using another rate)
              </el-checkbox>
            </el-form-item>

            <el-form-item
              v-if="actualAmountVisible"
              label="Reported Amount"
              prop="actualAmountInDefaultCurrency"
            >
              <money-input
                v-model="expense.actualAmountInDefaultCurrency"
                :currency="defaultCurrency"
              />
            </el-form-item>

            <el-form-item>
              <el-checkbox v-model="partialForBusiness">
                Expense is partially purposed for the business needs
              </el-checkbox>
            </el-form-item>

            <el-form-item
              v-if="percentOnBusinessVisible"
              label="% spent on business"
              prop="percentOnBusiness"
            >
              <el-input-number
                v-model="expense.percentOnBusiness"
                :min="0"
                :max="100"
              />
            </el-form-item>
          </div>

          <div class="col col-xs-12 col-lg-6">
            <h2>Additional notes</h2>

            <el-form-item
              label="Notes"
              prop="notes"
            >
              <el-input
                v-model="expense.notes"
                type="textarea"
                placeholder="Any additional information to be stored for this expense record"
                rows="5"
              />
            </el-form-item>

            <SaMarkdownOutput
              v-if="expense.notes"
              :source="expense.notes"
              preview
            />

            <h2>Attachments</h2>

            <documents-upload
              ref="documentsUpload"
              v-model="expense.uploads"
              form-property="uploads"
            />
          </div>
        </div>

        <hr>

        <div class="sa-buttons-bar">
          <el-button @click="navigateToExpensesOverview">
            Cancel
          </el-button>
          <el-button
            type="primary"
            @click="save"
          >
            Save
          </el-button>
        </div>
      </el-form>
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

  import { withGeneralTaxes } from '@/components/mixins/with-general-taxes';
  import withCategories from '@/components/mixins/with-categories';
  import SaMarkdownOutput from '@/components/SaMarkdownOutput';

  export default {
    name: 'EditExpense',

    components: {
      DocumentsUpload,
      CurrencyInput,
      MoneyInput,
      SaMarkdownOutput,
    },

    mixins: [withMediumDateFormatter, withGeneralTaxes, withCategories],

    props: {
      id: {
        type: Number,
        required: true,
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
          currency: { required: true, message: 'Please select a currency' },
          title: { required: true, message: 'Please provide the title' },
          datePaid: { required: true, message: 'Please provide the date when expense is paid' },
          originalAmount: { required: true, message: 'Please provide expense amount' },
        },
        alreadyConverted: false,
        reportedAnotherExchangeRate: false,
        partialForBusiness: false,
      };
    },

    computed: {
      ...mapState('workspaces', {
        workspace: 'currentWorkspace',
      }),

      isInDefaultCurrency() {
        return this.expense.currency === this.defaultCurrency;
      },

      defaultCurrency() {
        return this.workspace.defaultCurrency;
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
        const expenseResponse = await api.get(`/workspaces/${this.workspace.id}/expenses/${this.id}`);
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
          const attachments = await api.pageRequest(`/workspaces/${this.workspace.id}/documents`)
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
          await api.put(`/workspaces/${this.workspace.id}/expenses/${this.expense.id}`, expenseToPush);
        } else {
          await api.post(`/workspaces/${this.workspace.id}/expenses`, expenseToPush);
        }
        await this.$router.push({ name: 'expenses-overview' });
      },
    },
  };
</script>
