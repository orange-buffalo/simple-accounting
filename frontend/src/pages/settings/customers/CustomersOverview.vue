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

    <SaPageableItemsGql
      :page-query="(customersPageQuery as any)"
      path="workspace"
      :connection-accessor="connectionAccessor"
      :page-query-arguments="{ workspaceId: currentWorkspaceId }"
      #default="{ item }: { item: CustomerNode }"
    >
      <CustomersOverviewPanel :customer="item" />
    </SaPageableItemsGql>
  </div>
</template>

<script lang="ts" setup>
  import SaIcon from '@/components/SaIcon.vue';
  import useNavigation from '@/services/use-navigation';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import CustomersOverviewPanel from '@/pages/settings/customers/CustomersOverviewPanel.vue';
  import { $t } from '@/services/i18n';
  import { graphql } from '@/services/api/gql';
  import SaPageableItemsGql from '@/components/pageable-items/SaPageableItemsGql.vue';
  import type { CustomersPageQuery } from '@/services/api/gql/graphql';

  const customersPageQuery = graphql(`
    query customersPage($workspaceId: Int!, $first: Int!, $after: String) {
      workspace(id: $workspaceId) {
        customers(first: $first, after: $after) {
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

  type CustomerNode = CustomersPageQuery['workspace']['customers']['edges'][0]['node'];

  const connectionAccessor = (workspace: unknown) =>
    (workspace as CustomersPageQuery['workspace']).customers;

  const { navigateByViewName } = useNavigation();
  const navigateToCreateCustomerView = () => navigateByViewName('create-new-customer');

  const { currentWorkspaceId } = useCurrentWorkspace();
</script>
