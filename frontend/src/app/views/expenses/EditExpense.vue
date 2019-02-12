<template>
  <div>
    <div class="sa-page-header">
      <h1>{{pageHeader}}</h1>
    </div>

    <div class="sa-form">
      <el-form ref="expenseForm"
               :model="expense"
               label-position="right"
               label-width="200px"
               :rules="expenseValidationRules">

        <h2>General Information</h2>

        <el-form-item label="Category" prop="category">
          <el-select v-model="expense.category" placeholder="Select a category">
            <el-option
                v-for="category in categories"
                :key="category.id"
                :label="category.name"
                :value="category.id">
            </el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="Description / Title" prop="title">
          <el-input v-model="expense.title"
                    placeholder="Provide a short summary"/>
        </el-form-item>

        <el-form-item label="Currency" prop="currency">
          <currency-input v-model="expense.currency"/>
        </el-form-item>

        <el-form-item label="Amount" prop="originalAmount">
          <money-input v-model="expense.originalAmount"
                       :currency="expense.currency"/>
        </el-form-item>

        <el-form-item label="Date Paid" prop="datePaid">
          <!-- todo format from cldr https://github.com/ElemeFE/element/issues/11353 -->
          <el-date-picker
              v-model="expense.datePaid"
              type="date"
              placeholder="Date expense is paid"
              value-format="yyyy-MM-dd">
          </el-date-picker>
        </el-form-item>

        <el-form-item label="Tax" prop="tax">
          <el-select v-model="expense.tax" placeholder="Select a tax">
            <el-option
                v-for="tax in taxes"
                :key="tax.id"
                :label="tax.title"
                :value="tax.id">
            </el-option>
          </el-select>
        </el-form-item>

        <el-form-item v-if="!isInDefaultCurrency">
          <el-checkbox v-model="alreadyConverted">
            Already converted
          </el-checkbox>
        </el-form-item>

        <el-form-item :label="`Amount in ${defaultCurrency}`"
                      prop="amountInDefaultCurrency"
                      v-if="defaultCurrencyAmountVisible">
          <money-input v-model="expense.amountInDefaultCurrency"
                       :currency="defaultCurrency"></money-input>
        </el-form-item>

        <el-form-item v-if="alreadyConverted">
          <el-checkbox v-model="reportedAnotherExchangeRate">
            Reported converted amount is different (using another rate)
          </el-checkbox>
        </el-form-item>

        <el-form-item label="Reported Amount"
                      prop="actualAmountInDefaultCurrency"
                      v-if="actualAmountVisible">
          <money-input v-model="expense.actualAmountInDefaultCurrency"
                       :currency="defaultCurrency"></money-input>
        </el-form-item>

        <el-form-item>
          <el-checkbox v-model="partialForBusiness">
            Expense is partially purposed for the business needs
          </el-checkbox>
        </el-form-item>

        <el-form-item label="% spent on business"
                      prop="percentOnBusiness"
                      v-if="percentOnBusinessVisible">
          <el-input-number v-model="expense.percentOnBusiness"
                           :min="0"
                           :max="100"/>
        </el-form-item>

        <h2>Additional notes</h2>

        <el-form-item label="Notes" prop="notes">
          <el-input type="textarea" v-model="expense.notes"
                    placeholder="Any additional information to be stored for this expense record"/>
        </el-form-item>

        <h2>Attachments</h2>

        <documents-upload form-property="uploads"
                          ref="documentsUpload"
                          v-model="expense.uploads"/>
        <hr/>

        <div class="sa-buttons-bar">
          <el-button @click="navigateToExpensesOverview">Cancel</el-button>
          <el-button type="primary" @click="save">Save</el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script>
  import api from '@/services/api'
  import {mapState} from 'vuex'
  import DocumentsUpload from '@/app/components/DocumentsUpload'
  import CurrencyInput from '@/app/components/CurrencyInput'
  import MoneyInput from '@/app/components/MoneyInput'
  import {UploadsInfo} from '@/app/components/uploads-info'
  import withMediumDateFormatter from '@/app/components/mixins/with-medium-date-formatter'
  import {assign} from 'lodash'
  import {isNil} from 'lodash'
  import {withTaxes} from '@/app/components/mixins/with-taxes'

  export default {
    name: 'EditExpense',

    mixins: [withMediumDateFormatter, withTaxes],

    components: {
      DocumentsUpload,
      CurrencyInput,
      MoneyInput
    },

    data: function () {
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
          tax: null
        },
        expenseValidationRules: {
          currency: {required: true, message: 'Please select a currency'},
          title: {required: true, message: 'Please provide the title'},
          datePaid: {required: true, message: 'Please provide the date when expense is paid'},
          originalAmount: {required: true, message: 'Please provide expense amount'}
        },
        alreadyConverted: false,
        reportedAnotherExchangeRate: false,
        partialForBusiness: false
      }
    },

    created: async function () {
      if (this.$route.params.id) {
        let expenseResponse = await api.get(`/user/workspaces/${this.workspace.id}/expenses/${this.$route.params.id}`)
        this.expense = assign({}, this.expense, expenseResponse.data)

        this.alreadyConverted = this.expense.currency !== this.defaultCurrency
            && !isNil(this.expense.amountInDefaultCurrency)
            && this.expense.amountInDefaultCurrency > 0

        this.reportedAnotherExchangeRate = this.expense.currency !== this.defaultCurrency
            && !isNil(this.expense.actualAmountInDefaultCurrency)
            && (this.expense.actualAmountInDefaultCurrency !== this.expense.amountInDefaultCurrency)

        this.partialForBusiness = this.expense.percentOnBusiness !== 100

        if (this.expense.attachments && this.expense.attachments.length) {
          let attachments = await api.pageRequest(`/user/workspaces/${this.workspace.id}/documents`)
              .eager()
              .eqFilter("id", this.expense.attachments)
              .getPageData()
          attachments.forEach(attachment => this.expense.uploads.add(attachment))
        }
      }
    },

    computed: {
      ...mapState('workspaces', {
        workspace: 'currentWorkspace'
      }),

      ...mapState({
        categories: state => state.workspaces.currentWorkspace.categories.filter(category => category.expense)
      }),

      isInDefaultCurrency: function () {
        return this.expense.currency === this.defaultCurrency
      },

      defaultCurrency: function () {
        return this.workspace.defaultCurrency
      },

      defaultCurrencyAmountVisible: function () {
        return this.alreadyConverted && !this.isInDefaultCurrency
      },

      actualAmountVisible: function () {
        return this.defaultCurrencyAmountVisible && this.reportedAnotherExchangeRate && !this.isInDefaultCurrency
      },

      percentOnBusinessVisible: function () {
        return this.partialForBusiness
      },

      pageHeader: function () {
        return this.$route.params.id ? 'Edit Expense' : 'Record New Expense'
      }
    },

    methods: {
      navigateToExpensesOverview: function () {
        this.$router.push({name: 'expenses-overview'})
      },

      save: async function () {
        try {
          await this.$refs.expenseForm.validate();
        } catch (e) {
          return
        }

        try {
          await this.$refs.documentsUpload.submitUploads()
        } catch (e) {
          this.$message({
            showClose: true,
            message: 'Upload failed',
            type: 'error'
          });
          return
        }

        let expenseToPush = {
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
          tax: this.expense.tax
        }

        if (this.expense.id) {
          await api.put(`/user/workspaces/${this.workspace.id}/expenses/${this.expense.id}`, expenseToPush)
        } else {
          await api.post(`/user/workspaces/${this.workspace.id}/expenses`, expenseToPush)
        }
        this.$router.push({name: 'expenses-overview'})
      }
    }
  }
</script>