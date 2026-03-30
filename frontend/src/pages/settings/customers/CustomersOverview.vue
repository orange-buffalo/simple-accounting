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
      :page-query="customersPageQuery"
      path="customers"
      :page-query-arguments="{ workspaceId: currentWorkspaceId }"
      #default="{ item }"
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

  const customersPageQuery = graphql(`
    query customersPage($workspaceId: Int!, $first: Int!, $after: String) {
      customers(workspaceId: $workspaceId, first: $first, after: $after) {
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
  `);

  const { navigateByViewName } = useNavigation();
  const navigateToCreateCustomerView = () => navigateByViewName('create-new-customer');

  const { currentWorkspaceId } = useCurrentWorkspace();
</script>
