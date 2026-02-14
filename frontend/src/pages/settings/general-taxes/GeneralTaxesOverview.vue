<template>
  <div>
    <div class="sa-page-header">
      <h1>General Taxes</h1>

      <div class="sa-header-options">
        <div>
          <span>Filters coming soon</span>
        </div>

        <ElButton
          round
          @click="navigateToCreateTaxView"
        >
          <SaIcon icon="plus-thin" />
          Add new
        </ElButton>
      </div>
    </div>

    <SaPageableItems
      :page-provider="taxesProvider"
      #default="{ item: tax }"
    >
      <GeneralTaxOverviewPanel :tax="tax as GeneralTaxDto" />
    </SaPageableItems>
  </div>
</template>

<script lang="ts" setup>
import SaPageableItems from '@/components/pageable-items/SaPageableItems.vue';
import SaIcon from '@/components/SaIcon.vue';
import GeneralTaxOverviewPanel from '@/pages/settings/general-taxes/GeneralTaxOverviewPanel.vue';
import type { ApiPageRequest, GeneralTaxDto } from '@/services/api';
import { generalTaxesApi } from '@/services/api';
import useNavigation from '@/services/use-navigation';
import { useCurrentWorkspace } from '@/services/workspaces';

const { navigateByViewName } = useNavigation();
const navigateToCreateTaxView = () => navigateByViewName('create-new-general-tax');

const { currentWorkspaceId } = useCurrentWorkspace();
const taxesProvider = async (request: ApiPageRequest, config: RequestInit) =>
  generalTaxesApi.getTaxes(
    {
      ...request,
      workspaceId: currentWorkspaceId,
    },
    config,
  );
</script>
