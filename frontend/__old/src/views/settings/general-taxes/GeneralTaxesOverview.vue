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

    <DataItems
      ref="taxesList"
      #default="{item: tax}"
      :api-path="`/workspaces/${currentWorkspace.id}/general-taxes`"
    >
      <GeneralTaxOverviewPanel :tax="tax" />
    </DataItems>
  </div>
</template>

<script lang="ts">
  import { defineComponent } from '@vue/composition-api';
  import DataItems from '@/components/DataItems';
  import SaIcon from '@/components/SaIcon';
  import useNavigation from '@/components/navigation/useNavigation';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import GeneralTaxOverviewPanel from '@/views/settings/general-taxes/GeneralTaxOverviewPanel';

  export default defineComponent({
    components: {
      SaIcon,
      DataItems,
      GeneralTaxOverviewPanel,
    },

    setup() {
      const { navigateByViewName } = useNavigation();
      const navigateToCreateTaxView = () => navigateByViewName('create-new-general-tax');
      const { currentWorkspace } = useCurrentWorkspace();
      return {
        navigateToCreateTaxView,
        currentWorkspace,
      };
    },
  });
</script>
