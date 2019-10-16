<template>
  <div>
    <div class="sa-page-header">
      <h1>{{pageHeader}}</h1>
    </div>

    <div class="sa-form">
      <el-form ref="incomeForm"
               :model="income"
               label-position="right"
               :rules="incomeValidationRules">
        <div class="row">
          <div class="col col-xs-12 col-lg-6">
            <h2>General Information</h2>

            <el-form-item label="Category" prop="category">
              <el-select v-model="income.category" placeholder="Select a category">
                <el-option
                    v-for="category in categories"
                    :key="category.id"
                    :label="category.name"
                    :value="category.id">
                </el-option>
              </el-select>
            </el-form-item>

            <el-form-item label="Description / Title" prop="title">
              <el-input v-model="income.title"
                        placeholder="Provide a short summary"/>
            </el-form-item>

            <el-form-item label="Currency" prop="currency">
              <currency-input v-model="income.currency"/>
            </el-form-item>

            <el-form-item label="Amount" prop="originalAmount">
              <money-input v-model="income.originalAmount"
                           :currency="income.currency"/>
            </el-form-item>

            <el-form-item label="Date Paid" prop="datePaid">
              <!-- todo #78: format from cldr https://github.com/ElemeFE/element/issues/11353 -->
              <el-date-picker
                  v-model="income.dateReceived"
                  type="date"
                  placeholder="Date income is received"
                  value-format="yyyy-MM-dd">
              </el-date-picker>
            </el-form-item>

            <el-form-item v-if="!isInDefaultCurrency">
              <el-checkbox v-model="alreadyConverted">
                Already converted
              </el-checkbox>
            </el-form-item>

            <el-form-item :label="`Amount in ${defaultCurrency}`"
                          prop="amountInDefaultCurrency"
                          v-if="defaultCurrencyAmountVisible">
              <money-input v-model="income.amountInDefaultCurrency"
                           :currency="defaultCurrency"></money-input>
            </el-form-item>

            <el-form-item v-if="alreadyConverted">
              <el-checkbox v-model="reportedAnotherExchangeRate">
                Reported converted amount is different (using another rate)
              </el-checkbox>
            </el-form-item>

            <el-form-item label="Reported Amount"
                          prop="reportedAmountInDefaultCurrency"
                          v-if="reportedAmountVisible">
              <money-input v-model="income.reportedAmountInDefaultCurrency"
                           :currency="defaultCurrency"></money-input>
            </el-form-item>

            <el-form-item label="Added Tax" prop="tax">
              <el-select v-model="income.tax"
                         clearable
                         placeholder="Select a tax">
                <el-option
                    v-for="tax in taxes"
                    :key="tax.id"
                    :label="tax.title"
                    :value="tax.id">
                </el-option>
              </el-select>
            </el-form-item>
          </div>

          <div class="col col-xs-12 col-lg-6">
            <h2>Additional Information</h2>

            <el-form-item label="Linked Invoice"
                          prop="reportedAmountInDefaultCurrency"
                          v-if="income.linkedInvoice">
              <span>{{income.linkedInvoice.title}}</span>
            </el-form-item>

            <el-form-item label="Notes" prop="notes">
              <el-input type="textarea"
                        v-model="income.notes"
                        placeholder="Any additional information to be stored for this income record"
                        rows="5"/>
            </el-form-item>

            <SaMarkdownOutput :source="income.notes"
                              v-if="income.notes"
                              preview/>

            <h2>Attachments</h2>

            <documents-upload form-property="uploads"
                              ref="documentsUpload"
                              v-model="income.uploads"/>
          </div>
        </div>
        <hr/>

        <div class="sa-buttons-bar">
          <el-button @click="navigateToIncomesOverview">Cancel</el-button>
          <el-button type="primary" @click="save">Save</el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script>
  import api from '@/services/api'
  import {mapState} from 'vuex'
  import DocumentsUpload from '@/components/DocumentsUpload'
  import CurrencyInput from '@/components/CurrencyInput'
  import MoneyInput from '@/components/MoneyInput'
  import {UploadsInfo} from '@/components/uploads-info'
  import withMediumDateFormatter from '@/components/mixins/with-medium-date-formatter'
  import {assign} from 'lodash'
  import {isNil} from 'lodash'
  import {withTaxes} from '@/components/mixins/with-taxes'
  import withCategories from '@/components/mixins/with-categories'
  import SaMarkdownOutput from '@/components/SaMarkdownOutput'

  export default {
    name: 'EditIncome',

    mixins: [withMediumDateFormatter, withTaxes, withCategories],

    components: {
      DocumentsUpload,
      CurrencyInput,
      MoneyInput,
      SaMarkdownOutput
    },

    data: function () {
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
          tax: null
        },
        incomeValidationRules: {
          currency: {required: true, message: 'Please select a currency'},
          title: {required: true, message: 'Please provide the title'},
          dateReceived: {required: true, message: 'Please provide the date when income is received'},
          originalAmount: {required: true, message: 'Please provide income amount'}
        },
        alreadyConverted: false,
        reportedAnotherExchangeRate: false
      }
    },

    created: async function () {
      if (this.$route.params.id) {
        let incomeResponse = await api.get(`/workspaces/${this.workspace.id}/incomes/${this.$route.params.id}`)
        this.income = assign({}, this.income, incomeResponse.data)

        this.alreadyConverted = this.income.currency !== this.defaultCurrency
            && !isNil(this.income.amountInDefaultCurrency)
            && this.income.amountInDefaultCurrency > 0

        this.reportedAnotherExchangeRate = this.income.currency !== this.defaultCurrency
            && !isNil(this.income.reportedAmountInDefaultCurrency)
            && (this.income.reportedAmountInDefaultCurrency !== this.income.amountInDefaultCurrency)

        if (this.income.attachments && this.income.attachments.length) {
          let attachments = await api.pageRequest(`/workspaces/${this.workspace.id}/documents`)
              .eager()
              .eqFilter("id", this.income.attachments)
              .getPageData()
          attachments.forEach(attachment => this.income.uploads.add(attachment))
        }
      }
    },

    computed: {
      ...mapState('workspaces', {
        workspace: 'currentWorkspace'
      }),

      isInDefaultCurrency: function () {
        return this.income.currency === this.defaultCurrency
      },

      defaultCurrency: function () {
        return this.workspace.defaultCurrency
      },

      defaultCurrencyAmountVisible: function () {
        return this.alreadyConverted && !this.isInDefaultCurrency
      },

      reportedAmountVisible: function () {
        return this.defaultCurrencyAmountVisible && this.reportedAnotherExchangeRate && !this.isInDefaultCurrency
      },

      pageHeader: function () {
        return this.$route.params.id ? 'Edit Income' : 'Record New Income'
      }
    },

    methods: {
      navigateToIncomesOverview: function () {
        this.$router.push({name: 'incomes-overview'})
      },

      save: async function () {
        try {
          await this.$refs.incomeForm.validate();
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

        let incomeToPush = {
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
          tax: this.income.tax
        }

        if (this.income.id) {
          await api.put(`/workspaces/${this.workspace.id}/incomes/${this.income.id}`, incomeToPush)
        } else {
          await api.post(`/workspaces/${this.workspace.id}/incomes`, incomeToPush)
        }
        this.$router.push({name: 'incomes-overview'})
      }
    }
  }
</script>