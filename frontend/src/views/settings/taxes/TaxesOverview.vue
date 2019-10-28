<template>
  <div>
    <div class="sa-page-header">
      <h1>Taxes</h1>

      <div class="sa-header-options">
        <div>
          <span>Filters coming soon</span>
        </div>

        <el-button
          round
          @click="navigateToCreateTaxView"
        >
          <svgicon name="plus-thin" />
          Add new
        </el-button>
      </div>
    </div>

    <data-items
      ref="taxesList"
      :api-path="`/workspaces/${currentWorkspace.id}/taxes`"
    >
      <template slot-scope="scope">
        <tax-overview-panel :tax="scope.item" />
      </template>
    </data-items>
  </div>
</template>

<script>
import DataItems from '@/components/DataItems';
import TaxOverviewPanel from './TaxOverviewPanel';
import '@/components/icons/plus-thin';
import { withWorkspaces } from '@/components/mixins/with-workspaces';

export default {
  name: 'TaxesOverview',

  components: {
    DataItems,
    TaxOverviewPanel,
  },

  mixins: [withWorkspaces],

  methods: {
    navigateToCreateTaxView() {
      this.$router.push({ name: 'create-new-tax' });
    },
  },
};
</script>
