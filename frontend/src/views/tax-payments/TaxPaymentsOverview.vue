<template>
  <div>
    <div class="sa-page-header">
      <h1>Tax Payments</h1>

      <div class="sa-header-options">
        <div>
          <span>Filters coming soon</span>
        </div>

        <ElButton round
                  @click="navigateToCreateTaxPaymentView"
                  :disabled="!currentWorkspace.editable">
          <SaIcon icon="plus-thin"/>
          Add new
        </ElButton>
      </div>
    </div>

    <DataItems :api-path="`/workspaces/${currentWorkspace.id}/tax-payments`"
               #default="{item: taxPayment}">
      <TaxPaymentOverviewPanel :tax-payment="taxPayment"/>
    </DataItems>
  </div>
</template>

<script>
  import DataItems from '@/components/DataItems'
  import TaxPaymentOverviewPanel from './TaxPaymentOverviewPanel'
  import '@/components/icons/plus-thin'
  import {withWorkspaces} from '@/components/mixins/with-workspaces'
  import SaIcon from '@/components/SaIcon'

  export default {
    name: 'TaxPaymentsOverview',

    mixins: [withWorkspaces],

    components: {
      SaIcon,
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