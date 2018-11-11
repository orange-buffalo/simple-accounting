<template>
  <app-layout>
    <el-card>
      <el-form ref="expenseForm"
               :model="expense"
               label-position="right"
               label-width="200px"
               :rules="expenseValidationRules">

        <el-form-item label="category" prop="category">
          <el-select v-model="expense.category" placeholder="category">
            <el-option
                v-for="category in categories"
                :key="category.id"
                :label="category.name"
                :value="category.id">
            </el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="currency" prop="currency">
          <currency-input v-model="expense.currency"></currency-input>
        </el-form-item>

        <el-form-item label="originalAmount" prop="originalAmount">
          <money-input v-model="expense.originalAmount"
                       :currency="expense.currency"></money-input>
        </el-form-item>

        <el-form-item label="datePaid" prop="datePaid">
          <!-- todo format from cldr https://github.com/ElemeFE/element/issues/11353 -->
          <el-date-picker
              v-model="expense.datePaid"
              type="date"
              placeholder="Pick a Date"
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
          <money-input v-model="expense.amountInDefaultCurrency"
                       :currency="defaultCurrency"></money-input>
        </el-form-item>

        <el-form-item v-if="alreadyConverted">
          <el-checkbox v-model="reportedAnotherExchangeRate">
            Reported converted amount is different (use another rate)
          </el-checkbox>
        </el-form-item>

        <el-form-item label="Reported amount"
                      prop="actualAmountInDefaultCurrency"
                      v-if="actualAmountVisible">
          <money-input v-model="expense.actualAmountInDefaultCurrency"
                       :currency="defaultCurrency"></money-input>
        </el-form-item>

        <el-form-item label="percentOnBusinessInBps"
                      prop="percentOnBusinessInBps"
                      v-if="percentOnBusinessVisible">
          <el-input v-model="expense.percentOnBusinessInBps"></el-input>
        </el-form-item>

        <el-form-item label="notes" prop="notes">
          <el-input type="textarea" v-model="expense.notes"></el-input>
        </el-form-item>

        <document-upload @upload-complete="onNewAttachment">

        </document-upload>

        <el-form-item>
          <el-button type="primary" @click="save">Save</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </app-layout>
</template>

<script>

  import api from '@/services/api'
  import {mapMutations, mapState, mapGetters} from 'vuex'
  import DocumentUpload from '@/app/components/DocumentUpload'
  import CurrencyInput from '@/app/components/CurrencyInput'
  import MoneyInput from '@/app/components/MoneyInput'

  export default {
    name: 'CreateExpense',

    components: {
      DocumentUpload,
      CurrencyInput,
      MoneyInput
    },

    data: function () {
      return {
        expense: {
          category: null,
          currency: null,
          originalAmount: null,
          amountInDefaultCurrency: null,
          actualAmountInDefaultCurrency: null,
          attachments: [],
          percentOnBusinessInBps: null,
          notes: null,
          datePaid: new Date()
        },
        expenseValidationRules: {
          // name: [
          //   {required: true, message: 'Please input name', trigger: 'blur'}
          // ],
          // defaultCurrency: [
          //   {required: true, message: 'Please input currency', trigger: 'blur'}
          // ],
          // income: [
          //   {
          //     validator: (rule, value, callback) => {
          //       if (!this.category.income && !this.category.expense) {
          //         callback(new Error('At least one of income/expense must be selected'));
          //       }
          //       else {
          //         callback();
          //       }
          //     }
          //   }
          // ]
        },
        alreadyConverted: false,
        reportedAnotherExchangeRate: false,
        partialForBusiness: false
      }
    },

    computed: {
      ...mapState('workspaces', {
        workspace: 'currentWorkspace'
      }),

      ...mapState({
        categories: state => state.workspaces.currentWorkspace.categories.filter(category => category.expense)
      }),

      ...mapGetters({
        mediumDateFormatter: 'i18n/getMediumDateFormatter'
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
      }
    },

    methods: {
      save: function () {
        this.$refs.expenseForm.validate((valid) => {
          if (valid) {
            api
                .post(`/user/workspaces/${this.workspace.id}/expenses`, this.expense)
                .then(response => {
                  console.log(response)
                  this.$router.push({name: 'expenses-overview'})
                })
                .catch(() => {
                  this.$refs.expenseForm.clearValidate()
                  this.$message({
                    showClose: true,
                    message: 'Sorry, failed',
                    type: 'error'
                  });
                })
          }
        })
      },

      onNewAttachment: function (document) {
        // todo on destroy, delete all attachments if expense is not saved
        this.expense.attachments.push(document.id)
      }
    }
  }
</script>