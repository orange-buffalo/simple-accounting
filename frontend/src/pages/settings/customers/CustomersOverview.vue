<template>
  <SaOverviewPage
    v-model="freeSearchText"
    :header="$t.customersOverview.header()"
    :filter-placeholder="$t.customersOverview.filters.input.placeholder()"
    :create-action-label="$t.customersOverview.create()"
    create-action-view-name="create-new-customer"
  >
    <SaPageableItems
      :page-query="customersPageQuery"
      path="workspace.customers"
      :page-query-arguments="{ workspaceId: currentWorkspaceId, freeSearchText: freeSearchText || null }"
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

  const freeSearchText = ref<string | undefined>();
</script>
