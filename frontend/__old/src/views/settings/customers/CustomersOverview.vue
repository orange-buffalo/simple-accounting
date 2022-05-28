<template>
  <div>
    <div class="sa-page-header">
      <h1>Customers</h1>

      <div class="sa-header-options">
        <div>
          <span>Filters coming soon</span>
        </div>

        <ElButton
          round
          @click="navigateToCreateCustomerView"
        >
          <SaIcon icon="plus-thin" />
          Add new
        </ElButton>
      </div>
    </div>

    <DataItems
      ref="customersList"
      :api-path="`/workspaces/${currentWorkspace.id}/customers`"
      #default="{item}"
    >
      <CustomerOverviewPanel :customer="item" />
    </DataItems>
  </div>
</template>

<script lang="ts">
  import { defineComponent } from '@vue/composition-api';
  import DataItems from '@/components/DataItems';
  import SaIcon from '@/components/SaIcon';
  import useNavigation from '@/components/navigation/useNavigation';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import CustomerOverviewPanel from '@/views/settings/customers/CustomerOverviewPanel';

  export default defineComponent({
    components: {
      SaIcon,
      DataItems,
      CustomerOverviewPanel,
    },

    setup() {
      const { navigateByViewName } = useNavigation();
      const { currentWorkspace } = useCurrentWorkspace();

      const navigateToCreateCustomerView = () => navigateByViewName('create-new-customer');
      return {
        navigateToCreateCustomerView,
        currentWorkspace,
      };
    },
  });
</script>
