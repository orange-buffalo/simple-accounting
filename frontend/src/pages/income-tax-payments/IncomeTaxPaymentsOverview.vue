<template>
  <div>
    <div class="sa-page-header">
      <h1>{{ $t.incomeTaxPaymentsOverview.header() }}</h1>

      <div class="sa-header-options">
        <div>
          <span>{{ $t.incomeTaxPaymentsOverview.filters.announcement() }}</span>
        </div>

        <ElButton
          round
          :disabled="!currentWorkspace.editable"
          @click="navigateToCreateTaxPaymentView"
        >
          <SaIcon icon="plus-thin" />
          {{ $t.incomeTaxPaymentsOverview.create() }}
        </ElButton>
      </div>
    </div>

    <SaPageableItems
      #default="{ item: taxPayment }"
      :page-provider="taxPaymentsProvider"
    >
      <IncomeTaxPaymentsOverviewPanel :tax-payment="taxPayment as IncomeTaxPaymentDto" />
    </SaPageableItems>
  </div>
</template>

<script lang="ts" setup>
import SaPageableItems from '@/components/pageable-items/SaPageableItems.vue';
import SaIcon from '@/components/SaIcon.vue';
import IncomeTaxPaymentsOverviewPanel from '@/pages/income-tax-payments/IncomeTaxPaymentsOverviewPanel.vue';
import type { ApiPageRequest, IncomeTaxPaymentDto } from '@/services/api';
import { incomeTaxPaymentsApi } from '@/services/api';
import { $t } from '@/services/i18n';
import useNavigation from '@/services/use-navigation';
import { useCurrentWorkspace } from '@/services/workspaces';

const { navigateByViewName } = useNavigation();
const navigateToCreateTaxPaymentView = () => navigateByViewName('create-new-income-tax-payment');
const { currentWorkspace, currentWorkspaceId } = useCurrentWorkspace();

const taxPaymentsProvider = async (request: ApiPageRequest, config: RequestInit) =>
  incomeTaxPaymentsApi.getTaxPayments(
    {
      ...request,
      workspaceId: currentWorkspaceId,
    },
    config,
  );
</script>
