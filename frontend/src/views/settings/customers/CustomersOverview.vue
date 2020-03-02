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
    >
      <template slot-scope="scope">
        <CustomerOverviewPanel :customer="scope.item" />
      </template>
    </DataItems>
  </div>
</template>

<script>
  import DataItems from '@/components/DataItems';
  import SaIcon from '@/components/SaIcon';
  import withWorkspaces from '@/components/mixins/with-workspaces';
  import CustomerOverviewPanel from './CustomerOverviewPanel';

  export default {
    name: 'CustomersOverview',

    components: {
      SaIcon,
      DataItems,
      CustomerOverviewPanel,
    },

    mixins: [withWorkspaces],

    methods: {
      navigateToCreateCustomerView() {
        this.$router.push({ name: 'create-new-customer' });
      },
    },
  };
</script>
