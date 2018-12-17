<template>
  <div>
    <div class="sa-page-header">
      <h1>Customers</h1>

      <div class="sa-header-options">
        <div>
          <span>Filters coming soon</span>

        </div>

        <el-button round
                   @click="navigateToCreateCustomerView">
          <svgicon name="plus-thin"/>
          Add new
        </el-button>
      </div>
    </div>

    <data-items :api-path="`/user/workspaces/${workspaceId}/customers`"
                ref="customersList">
      <template slot-scope="scope">
        <customer-overview-panel :customer="scope.item"/>
      </template>
    </data-items>
  </div>
</template>

<script>
  import DataItems from '@/components/DataItems'
  import {mapState} from 'vuex'
  import CustomerOverviewPanel from './CustomerOverviewPanel'
  import '@/components/icons/plus-thin'

  export default {
    name: 'CustomersOverview',

    components: {
      DataItems,
      CustomerOverviewPanel
    },

    data: function () {
      return {}
    },

    computed: {
      ...mapState({
        workspaceId: state => state.workspaces.currentWorkspace.id
      })
    },

    methods: {
      navigateToCreateCustomerView: function () {
        this.$router.push({name: 'create-new-customer'})
      }
    }
  }
</script>