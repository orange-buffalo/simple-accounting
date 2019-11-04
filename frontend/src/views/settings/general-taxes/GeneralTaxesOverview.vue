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
      :api-path="`/workspaces/${currentWorkspace.id}/general-taxes`"
      #default="{item: tax}"
    >
      <GeneralTaxOverviewPanel :tax="tax" />
    </DataItems>
  </div>
</template>

<script>
  import DataItems from '@/components/DataItems';
  import GeneralTaxOverviewPanel from './GeneralTaxOverviewPanel';
  import { withWorkspaces } from '@/components/mixins/with-workspaces';
  import SaIcon from '@/components/SaIcon';

  export default {
    name: 'GeneralTaxesOverview',

    components: {
      SaIcon,
      DataItems,
      GeneralTaxOverviewPanel,
    },

    mixins: [withWorkspaces],

    methods: {
      navigateToCreateTaxView() {
        this.$router.push({ name: 'create-new-general-tax' });
      },
    },
  };
</script>
