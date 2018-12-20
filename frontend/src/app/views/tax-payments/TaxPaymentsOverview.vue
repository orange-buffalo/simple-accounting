<template>
  <div>
    <div class="sa-page-header">
      <h1>Tax Payments</h1>

      <div class="sa-header-options">
        <div>
          <span>Filters coming soon</span>
        </div>

        <el-button round
                   @click="navigateToCreateTaxPaymentView">
          <svgicon name="plus-thin"/>
          Add new
        </el-button>
      </div>
    </div>

    <data-items :api-path="`/user/workspaces/${currentWorkspace.id}/tax-payments`">
      <template slot-scope="scope">
        <tax-payment-overview-panel :tax-payment="scope.item"/>
      </template>
    </data-items>
  </div>
</template>

<script>
  import DataItems from '@/components/DataItems'
  import TaxPaymentOverviewPanel from './TaxPaymentOverviewPanel'
  import '@/components/icons/plus-thin'
  import {withWorkspaces} from '@/app/components/mixins/with-workspaces'

  export default {
    name: 'TaxPaymentsOverview',

    mixins: [withWorkspaces],

    components: {
      DataItems,
      TaxPaymentOverviewPanel
    },

    methods: {
      navigateToCreateTaxPaymentView: function () {
        this.$router.push({name: 'create-new-tax-payment'})
      }
    }
  }
</script>