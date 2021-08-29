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

<script lang="ts">
  import { defineComponent } from '@vue/composition-api';
  import DataItems from '@/components/DataItems';
  import SaIcon from '@/components/SaIcon';
  import IncomeTaxPaymentsOverviewPanel from '@/views/income-tax-payments/IncomeTaxPaymentsOverviewPanel';
  import useNavigation from '@/components/navigation/useNavigation';
  import { useCurrentWorkspace } from '@/services/workspaces';

  export default defineComponent({
    components: {
      IncomeTaxPaymentsOverviewPanel,
      SaIcon,
      DataItems,
    },

    setup() {
      const { navigateByViewName } = useNavigation();
      const { currentWorkspace } = useCurrentWorkspace();
      const navigateToCreateTaxPaymentView = () => navigateByViewName('create-new-income-tax-payment');
      return {
        navigateToCreateTaxPaymentView,
        currentWorkspace,
      };
    },
  });
</script>
