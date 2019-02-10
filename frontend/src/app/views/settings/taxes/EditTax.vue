<template>
  <div>
    <div class="sa-page-header">
      <h1>{{pageHeader}}</h1>
    </div>

    <div class="sa-form">
      <el-form ref="taxForm"
               :model="tax"
               label-position="right"
               label-width="200px"
               :rules="taxValidationRules">

        <h2>General Information</h2>

        <el-form-item label="Title" prop="title">
          <el-input v-model="tax.title" placeholder="Provide a title of the tax"/>
        </el-form-item>

        <el-form-item label="Description" prop="description">
          <el-input v-model="tax.description" placeholder="Short description of a tax"/>
        </el-form-item>

        <!--todo: input in bps-->
        <el-form-item label="Rate" prop="rateInBps">
          <el-input v-model="tax.rateInBps" placeholder="Provide a rate for this tax"/>
        </el-form-item>

        <hr/>

        <div class="sa-buttons-bar">
          <el-button @click="navigateToTaxesOverview">Cancel</el-button>
          <el-button type="primary" @click="save">Save</el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script>
  import api from '@/services/api'
  import {mapState} from 'vuex'
  import {assign} from 'lodash'

  export default {
    name: 'EditTax',

    components: {
    },

    data: function () {
      return {
        tax: {
          name: null
        },
        taxValidationRules: {
          title: {required: true, message: 'Please provide a title'},
          rateInBps: {required: true, message: 'Please provide the rate'}
        }
      }
    },

    created: async function () {
      if (this.$route.params.id) {
        let incomeResponse = await api.get(`/user/workspaces/${this.workspace.id}/taxes/${this.$route.params.id}`)
        this.tax = assign({}, this.tax, incomeResponse.data)
      }
    },

    computed: {
      ...mapState('workspaces', {
        workspace: 'currentWorkspace'
      }),

      pageHeader: function () {
        return this.$route.params.id ? 'Edit Tax' : 'Create New Tax'
      }
    },

    methods: {
      navigateToTaxesOverview: function () {
        this.$router.push({name: 'taxes-overview'})
      },

      save: async function () {
        try {
          await this.$refs.taxForm.validate();
        } catch (e) {
          return
        }

        let taxToPush = {
          title: this.tax.title,
          description: this.tax.description,
          rateInBps: this.tax.rateInBps
        }

        if (this.tax.id) {
          await api.put(`/user/workspaces/${this.workspace.id}/taxes/${this.tax.id}`, taxToPush)
        } else {
          await api.post(`/user/workspaces/${this.workspace.id}/taxes`, taxToPush)
        }
        this.$router.push({name: 'taxes-overview'})
      }
    }
  }
</script>