<template>
  <app-layout>
    <el-card>
      <el-form ref="expenseForm"
               :model="expense"
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

        <el-form-item label="notes" prop="notes">
          <el-input type="textarea" v-model="expense.notes"></el-input>
        </el-form-item>
        <el-form-item label="currency" prop="currency">
          <el-input v-model="expense.currency"></el-input>
        </el-form-item>
        <el-form-item label="originalAmount" prop="originalAmount">
          <el-input v-model="expense.originalAmount"></el-input>
        </el-form-item>
        <el-form-item label="amountInDefaultCurrency" prop="amountInDefaultCurrency">
          <el-input v-model="expense.amountInDefaultCurrency"></el-input>
        </el-form-item>
        <el-form-item label="actualAmountInDefaultCurrency" prop="actualAmountInDefaultCurrency">
          <el-input v-model="expense.actualAmountInDefaultCurrency"></el-input>
        </el-form-item>
        <el-form-item label="percentOnBusinessInBps" prop="percentOnBusinessInBps">
          <el-input v-model="expense.percentOnBusinessInBps"></el-input>
        </el-form-item>
        <el-form-item label="datePaid" prop="datePaid">
          <el-date-picker
              v-model="expense.datePaid"
              type="date"
              placeholder="Pick a Date"
              value-format="yyyy-MM-dd">
          </el-date-picker>
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
  import {mapMutations, mapState} from 'vuex'
  import DocumentUpload from '@/app/components/DocumentUpload'

  export default {
    name: 'CreateExpense',

    components: {
      DocumentUpload
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
          datePaid: null
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
        }
      }
    },

    computed: {
      ...mapState('workspaces', {
        workspace: 'currentWorkspace'
      }),
      ...mapState({
        categories: state => state.workspaces.currentWorkspace.categories
      })
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