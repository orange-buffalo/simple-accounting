<template>
  <div>
    <div class="sa-page-header">
      <h1>Income Tax Payments</h1>

      <div class="sa-header-options">
        <div>
          <span>Filters coming soon</span>
        </div>

        <ElButton
          round
          :disabled="!currentWorkspace.editable"
          @click="navigateToCreateTaxPaymentView"
        >
          <SaIcon icon="plus-thin" />
          Add new
        </ElButton>
      </div>
    </div>

    <DataItems
      :api-path="`/workspaces/${currentWorkspace.id}/income-tax-payments`"
      #default="{item: taxPayment}"
    >
      <IncomeTaxPaymentOverviewPanel :tax-payment="taxPayment" />
    </DataItems>
  </div>
</template>

<script>
import DataItems from '@/components/DataItems';
import { withWorkspaces } from '@/components/mixins/with-workspaces';
import SaIcon from '@/components/SaIcon';
import IncomeTaxPaymentOverviewPanel from '@/views/income-tax-payments/IncomeTaxPaymentOverviewPanel';

export default {
  name: 'TaxPaymentsOverview',

  components: {
    IncomeTaxPaymentOverviewPanel,
    SaIcon,
    DataItems,
  },

  mixins: [withWorkspaces],

  methods: {
    navigateToCreateTaxPaymentView() {
      this.$router.push({ name: 'create-new-income-tax-payment' });
    },
  },
};
</script>
