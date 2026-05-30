<template>
  <SaOverviewPage
    v-model="overviewFilters"
    :header="$t.customersOverview.header()"
    :filters="customerFilters"
    :create-action-label="$t.customersOverview.create()"
    create-action-view-name="create-new-customer"
  >
    <SaPageableItems
      :page-query="customersPageQuery"
      path="workspace.customers"
      :page-query-arguments="{ workspaceId: currentWorkspaceId, freeSearchText: overviewFilters.freeSearchText }"
      #default="{ item }"
    >
      <CustomersOverviewPanel :customer="item" />
    </SaPageableItems>
  </SaOverviewPage>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import SaOverviewPage from '@/components/SaOverviewPage.vue';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import CustomersOverviewPanel from '@/pages/settings/customers/CustomersOverviewPanel.vue';
  import { $t } from '@/services/i18n';
  import { graphql } from '@/services/api/gql';
  import SaPageableItems from '@/components/pageable-items/SaPageableItems.vue';
  import {
    createOverviewFilters,
    type SaOverviewFilterConfigs,
  } from '@/components/overview-page/overview-page-filters';

  type CustomersOverviewFilters = {
    freeSearchText: string | null,
  };

  const customersPageQuery = graphql(`
    query customersPage($workspaceId: String!, $first: Int!, $after: String, $freeSearchText: String) {
      workspace(id: $workspaceId) {
        customers(first: $first, after: $after, freeSearchText: $freeSearchText) {
          edges {
            cursor
            node {
              id
              name
            }
          }
          pageInfo {
            ...PaginationPageInfo
          }
          totalCount
        }
      }
    }
  `);

  const { currentWorkspaceId } = useCurrentWorkspace();

  const overviewFilters = ref(createOverviewFilters<CustomersOverviewFilters>({
    freeSearchText: null,
  }));
  const customerFilters: SaOverviewFilterConfigs<CustomersOverviewFilters> = {
    freeSearchText: {
      type: 'text',
      label: $t.value.customersOverview.filters.freeSearchText.label(),
    },
  };
</script>
