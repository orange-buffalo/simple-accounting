<template>
  <div>
    <div class="page-header">
      <h1>Customers</h1>

      <div class="header-options">
        <div>
          <span>Filters coming soon</span>

        </div>

        <el-button round
                   @click="navigateToCreateCustomerView">
          <plus-icon/>
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
  import PlusIcon from 'vue-material-design-icons/Plus'

  export default {
    name: 'CustomersOverview',

    components: {
      DataItems,
      CustomerOverviewPanel,
      PlusIcon
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