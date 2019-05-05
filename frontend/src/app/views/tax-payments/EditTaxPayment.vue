<template>
  <div>
    <div class="sa-page-header">
      <h1>{{pageHeader}}</h1>
    </div>

    <div class="sa-form">
      <el-form ref="taxPaymentForm"
               :model="taxPayment"
               label-position="right"
               label-width="200px"
               :rules="taxPaymentValidationRules">

        <h2>General Information</h2>

        <el-form-item label="Description / Title" prop="title">
          <el-input v-model="taxPayment.title"
                    placeholder="Provide a short summary"/>
        </el-form-item>

        <el-form-item label="Amount" prop="amount">
          <money-input v-model="taxPayment.amount"
                       :currency="defaultCurrency"/>
        </el-form-item>

        <el-form-item label="Date Paid" prop="datePaid">
          <!-- todo #78: format from cldr https://github.com/ElemeFE/element/issues/11353 -->
          <el-date-picker
              v-model="taxPayment.datePaid"
              type="date"
              placeholder="Date tax is paid"
              value-format="yyyy-MM-dd">
          </el-date-picker>
        </el-form-item>

        <el-form-item label="Reporting Date" prop="reportingDate">
          <!-- todo #78: format from cldr https://github.com/ElemeFE/element/issues/11353 -->
          <el-date-picker
              v-model="taxPayment.reportingDate"
              type="date"
              placeholder="Date to include this payment into reporting by"
              value-format="yyyy-MM-dd">
          </el-date-picker>
        </el-form-item>

        <h2>Additional notes</h2>

        <el-form-item label="Notes" prop="notes">
          <el-input type="textarea" v-model="taxPayment.notes"
                    placeholder="Any additional information to be stored for this tax payment record"/>
        </el-form-item>

        <h2>Attachments</h2>

        <documents-upload form-property="uploads"
                          ref="documentsUpload"
                          v-model="taxPayment.uploads"/>
        <hr/>

        <div class="sa-buttons-bar">
          <el-button @click="navigateToTaxPaymentsOverview">Cancel</el-button>
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

  export default {
    name: 'EditTaxPayment',

    mixins: [withMediumDateFormatter],

    components: {
      DocumentsUpload,
      CurrencyInput,
      MoneyInput
    },

    data: function () {
      return {
        taxPayment: {
          title: null,
          amount: null,
          attachments: [],
          notes: null,
          datePaid: new Date(),
          uploads: new UploadsInfo(),
          reportingDate: null
        },
        taxPaymentValidationRules: {
          title: {required: true, message: 'Please provide the title'},
          datePaid: {required: true, message: 'Please provide the date when tax payment is done'},
          amount: {required: true, message: 'Please provide tax payment amount'}
        }
      }
    },

    created: async function () {
      if (this.$route.params.id) {
        let taxPaymentResponse = await api.get(`/user/workspaces/${this.workspace.id}/tax-payments/${this.$route.params.id}`)
        this.taxPayment = assign({}, this.taxPayment, taxPaymentResponse.data)

        if (this.taxPayment.attachments && this.taxPayment.attachments.length) {
          let attachments = await api.pageRequest(`/user/workspaces/${this.workspace.id}/documents`)
              .eager()
              .eqFilter("id", this.taxPayment.attachments)
              .getPageData()
          attachments.forEach(attachment => this.taxPayment.uploads.add(attachment))
        }
      }
    },

    computed: {
      ...mapState('workspaces', {
        workspace: 'currentWorkspace'
      }),

      defaultCurrency: function () {
        return this.workspace.defaultCurrency
      },

      pageHeader: function () {
        return this.$route.params.id ? 'Edit Tax Payment' : 'Record New Tax Payment'
      }
    },

    methods: {
      navigateToTaxPaymentsOverview: function () {
        this.$router.push({name: 'tax-payments-overview'})
      },

      save: async function () {
        try {
          await this.$refs.taxPaymentForm.validate();
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

        let taxPaymentToPush = {
          datePaid: this.taxPayment.datePaid,
          title: this.taxPayment.title,
          amount: this.taxPayment.amount,
          attachments: this.taxPayment.uploads.getDocumentsIds(),
          notes: this.taxPayment.notes,
          reportingDate: this.taxPayment.reportingDate
        }

        if (this.taxPayment.id) {
          await api.put(`/user/workspaces/${this.workspace.id}/tax-payments/${this.taxPayment.id}`, taxPaymentToPush)
        } else {
          await api.post(`/user/workspaces/${this.workspace.id}/tax-payments`, taxPaymentToPush)
        }
        this.$router.push({name: 'tax-payments-overview'})
      }
    }
  }
</script>