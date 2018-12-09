<template>
  <div>
    <div class="sa-page-header">
      <h1>{{pageHeader}}</h1>
    </div>

    <div class="sa-form">
      <el-form ref="customerForm"
               :model="customer"
               label-position="right"
               label-width="200px"
               :rules="customerValidationRules">

        <h2>General Information</h2>

        <el-form-item label="Name" prop="name">
          <el-input v-model="customer.name" placeholder="Provide a name of the customer"/>
        </el-form-item>

        <hr/>

        <div class="sa-buttons-bar">
          <el-button @click="navigateToCustomersOverview">Cancel</el-button>
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
    name: 'EditCustomer',

    components: {
    },

    data: function () {
      return {
        customer: {
          name: null
        },
        customerValidationRules: {
          name: {required: true, message: 'Please select a name'}
        }
      }
    },

    created: async function () {
      if (this.$route.params.id) {
        let incomeResponse = await api.get(`/user/workspaces/${this.workspace.id}/customers/${this.$route.params.id}`)
        this.customer = assign({}, this.customer, incomeResponse.data)
      }
    },

    computed: {
      ...mapState('workspaces', {
        workspace: 'currentWorkspace'
      }),

      pageHeader: function () {
        return this.$route.params.id ? 'Edit Customer' : 'Create New Customer'
      }
    },

    methods: {
      navigateToCustomersOverview: function () {
        this.$router.push({name: 'customers-overview'})
      },

      save: async function () {
        try {
          await this.$refs.customerForm.validate();
        } catch (e) {
          return
        }

        let customerToPush = {
          name: this.customer.name
        }

        if (this.customer.id) {
          await api.put(`/user/workspaces/${this.workspace.id}/customers/${this.customer.id}`, customerToPush)
        } else {
          await api.post(`/user/workspaces/${this.workspace.id}/customers`, customerToPush)
        }
        this.$router.push({name: 'customers-overview'})
      }
    }
  }
</script>