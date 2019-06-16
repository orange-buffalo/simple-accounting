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

    <data-items :api-path="`/workspaces/${currentWorkspace.id}/customers`"
                ref="customersList">
      <template slot-scope="scope">
        <customer-overview-panel :customer="scope.item"/>
      </template>
    </data-items>
  </div>
</template>

<script>
  import DataItems from '@/components/DataItems'
  import CustomerOverviewPanel from './CustomerOverviewPanel'
  import '@/components/icons/plus-thin'
  import {withWorkspaces} from '@/components/mixins/with-workspaces'

  export default {
    name: 'CustomersOverview',

    mixins: [withWorkspaces],

    components: {
      DataItems,
      CustomerOverviewPanel
    },

    methods: {
      navigateToCreateCustomerView: function () {
        this.$router.push({name: 'create-new-customer'})
      }
    }
  }
</script>