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
              label="Date Paid"
              prop="datePaid"
            >
              <!-- todo #78: format from cldr https://github.com/ElemeFE/element/issues/11353 -->
              <ElDatePicker
                v-model="income.dateReceived"
                type="date"
                placeholder="Date income is received"
                value-format="yyyy-MM-dd"
              />
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
                v-model="income.amountInDefaultCurrency"
                :currency="defaultCurrency"
              />
            </ElFormItem>

            <ElFormItem v-if="alreadyConverted">
              <ElCheckbox v-model="reportedAnotherExchangeRate">
                Reported converted amount is different (using another rate)
              </ElCheckbox>
            </ElFormItem>

            <ElFormItem
              v-if="reportedAmountVisible"
              label="Reported Amount"
              prop="reportedAmountInDefaultCurrency"
            >
              <MoneyInput
                v-model="income.reportedAmountInDefaultCurrency"
                :currency="defaultCurrency"
              />
            </ElFormItem>

            <ElFormItem
              label="Added General Tax"
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
              <ElInput
                v-model="income.notes"
                type="textarea"
                placeholder="Any additional information to be stored for this income record"
                rows="5"
              />
            </ElFormItem>

            <SaMarkdownOutput
              v-if="income.notes"
              :source="income.notes"
              preview
            />

            <h2>Attachments</h2>

            <DocumentsUpload
              ref="documentsUpload"
              v-model="income.uploads"
              form-property="uploads"
            />
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
    name: 'EditIncome',

    components: {
      DocumentsUpload,
      CurrencyInput,
      MoneyInput,
      SaMarkdownOutput,
    },

    mixins: [withMediumDateFormatter, withGeneralTaxes, withCategories],

    data() {
      return {
        income: {
          category: null,
          title: null,
          currency: null,
          originalAmount: null,
          amountInDefaultCurrency: null,
          reportedAmountInDefaultCurrency: null,
          attachments: [],
          notes: null,
          dateReceived: new Date(),
          uploads: new UploadsInfo(),
          generalTax: null,
        },
        incomeValidationRules: {
          currency: { required: true, message: 'Please select a currency' },
          title: { required: true, message: 'Please provide the title' },
          dateReceived: { required: true, message: 'Please provide the date when income is received' },
          originalAmount: { required: true, message: 'Please provide income amount' },
        },
        alreadyConverted: false,
        reportedAnotherExchangeRate: false,
      };
    },

    async created() {
      if (this.$route.params.id) {
        const incomeResponse = await api.get(`/workspaces/${this.workspace.id}/incomes/${this.$route.params.id}`);
        this.income = assign({}, this.income, incomeResponse.data);

        this.alreadyConverted = this.income.currency !== this.defaultCurrency
          && !isNil(this.income.amountInDefaultCurrency)
          && this.income.amountInDefaultCurrency > 0;

        this.reportedAnotherExchangeRate = this.income.currency !== this.defaultCurrency
          && !isNil(this.income.reportedAmountInDefaultCurrency)
          && (this.income.reportedAmountInDefaultCurrency !== this.income.amountInDefaultCurrency);

        if (this.income.attachments && this.income.attachments.length) {
          const attachments = await api.pageRequest(`/workspaces/${this.workspace.id}/documents`)
            .eager()
            .eqFilter('id', this.income.attachments)
            .getPageData();
          attachments.forEach(attachment => this.income.uploads.add(attachment));
        }
      }
    },

    computed: {
      ...mapState('workspaces', {
        workspace: 'currentWorkspace',
      }),

      isInDefaultCurrency() {
        return this.income.currency === this.defaultCurrency;
      },

      defaultCurrency() {
        return this.workspace.defaultCurrency;
      },

      defaultCurrencyAmountVisible() {
        return this.alreadyConverted && !this.isInDefaultCurrency;
      },

      reportedAmountVisible() {
        return this.defaultCurrencyAmountVisible && this.reportedAnotherExchangeRate && !this.isInDefaultCurrency;
      },

      pageHeader() {
        return this.$route.params.id ? 'Edit Income' : 'Record New Income';
      },
    },

    methods: {
      navigateToIncomesOverview() {
        this.$router.push({ name: 'incomes-overview' });
      },

      async save() {
        try {
          await this.$refs.incomeForm.validate();
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

        const incomeToPush = {
          category: this.income.category,
          dateReceived: this.income.dateReceived,
          title: this.income.title,
          currency: this.income.currency,
          originalAmount: this.income.originalAmount,
          amountInDefaultCurrency: this.alreadyConverted ? this.income.amountInDefaultCurrency : null,
          reportedAmountInDefaultCurrency: this.reportedAnotherExchangeRate
            ? this.income.reportedAmountInDefaultCurrency : this.income.amountInDefaultCurrency,
          attachments: this.income.uploads.getDocumentsIds(),
          notes: this.income.notes,
          generalTax: this.income.generalTax,
        };

        if (this.income.id) {
          await api.put(`/workspaces/${this.workspace.id}/incomes/${this.income.id}`, incomeToPush);
        } else {
          await api.post(`/workspaces/${this.workspace.id}/incomes`, incomeToPush);
        }
        await this.$router.push({ name: 'incomes-overview' });
      },
    },
  };
</script>
