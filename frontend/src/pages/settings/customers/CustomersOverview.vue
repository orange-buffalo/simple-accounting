<template>
  <div>
    <div class="sa-page-header">
      <h1>{{ $t.customersOverview.header() }}</h1>

      <div class="sa-header-options">
        <div>
          <span>{{ $t.customersOverview.filters.announcement() }}</span>
        </div>

        <ElButton
          round
          @click="navigateToCreateCustomerView"
        >
          <SaIcon icon="plus-thin" />
          {{ $t.customersOverview.create() }}
        </ElButton>
      </div>
    </div>

    <SaPageableItems
      :page-provider="customersProvider"
      #default="{ item }"
    >
      <CustomersOverviewPanel :customer="item as CustomerDto" />
    </SaPageableItems>
  </div>
</template>

<script lang="ts" setup>
import SaPageableItems from '@/components/pageable-items/SaPageableItems.vue';
import SaIcon from '@/components/SaIcon.vue';
import CustomersOverviewPanel from '@/pages/settings/customers/CustomersOverviewPanel.vue';
import type { ApiPageRequest, CustomerDto } from '@/services/api';
import { customersApi } from '@/services/api';
import { $t } from '@/services/i18n';
import useNavigation from '@/services/use-navigation';
import { useCurrentWorkspace } from '@/services/workspaces';

const { navigateByViewName } = useNavigation();
const navigateToCreateCustomerView = () => navigateByViewName('create-new-customer');

const { currentWorkspaceId } = useCurrentWorkspace();
const customersProvider = async (request: ApiPageRequest, config: RequestInit) =>
  customersApi.getCustomers(
    {
      workspaceId: currentWorkspaceId,
      ...request,
    },
    config,
  );
</script>
