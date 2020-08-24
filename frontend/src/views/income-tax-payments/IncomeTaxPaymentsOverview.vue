<template>
  <div>
    <div class="sa-page-header">
      <h1>{{ $t('incomeTaxPaymentsOverview.header') }}</h1>

      <div class="sa-header-options">
        <div>
          <span>{{ $t('incomeTaxPaymentsOverview.filters.announcement') }}</span>
        </div>

        <ElButton
          round
          :disabled="!currentWorkspace.editable"
          @click="navigateToCreateTaxPaymentView"
        >
          <SaIcon icon="plus-thin" />
          {{ $t('incomeTaxPaymentsOverview.create') }}
        </ElButton>
      </div>
    </div>

    <DataItems
      #default="{item: taxPayment}"
      :api-path="`/workspaces/${currentWorkspace.id}/income-tax-payments`"
    >
      <IncomeTaxPaymentsOverviewPanel :tax-payment="taxPayment" />
    </DataItems>
  </div>
</template>

<script>
  import DataItems from '@/components/DataItems';
  import withWorkspaces from '@/components/mixins/with-workspaces';
  import SaIcon from '@/components/SaIcon';
  import IncomeTaxPaymentsOverviewPanel from '@/views/income-tax-payments/IncomeTaxPaymentsOverviewPanel';

  export default {
    name: 'TaxPaymentsOverview',

    components: {
      IncomeTaxPaymentsOverviewPanel,
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
