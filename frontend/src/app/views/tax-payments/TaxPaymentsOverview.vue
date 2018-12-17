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

    <data-items :api-path="`/user/workspaces/${workspaceId}/tax-payments`">
      <template slot-scope="scope">
        <tax-payment-overview-panel :tax-payment="scope.item"/>
      </template>
    </data-items>
  </div>
</template>

<script>
  import DataItems from '@/components/DataItems'
  import {mapState} from 'vuex'
  import withMediumDateFormatter from '@/app/components/mixins/with-medium-date-formatter'
  import TaxPaymentOverviewPanel from './TaxPaymentOverviewPanel'
  import '@/components/icons/plus-thin'

  export default {
    name: 'TaxPaymentsOverview',

    mixins: [withMediumDateFormatter],

    components: {
      DataItems,
      TaxPaymentOverviewPanel
    },

    data: function () {
      return {}
    },

    computed: {
      ...mapState({
        workspaceId: state => state.workspaces.currentWorkspace.id,
        defaultCurrency: state => state.workspaces.currentWorkspace.defaultCurrency
      })
    },

    methods: {
      navigateToCreateTaxPaymentView: function () {
        this.$router.push({name: 'create-new-tax-payment'})
      }
    }
  }
</script>