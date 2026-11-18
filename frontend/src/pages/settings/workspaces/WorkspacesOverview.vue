<template>
  <SaOverviewPage
    :header="$t.workspacesOverview.header()"
    :create-action-label="$t.workspacesOverview.create()"
    :create-action-view-name="CREATE_NEW_WORKSPACE_ROUTE_NAME"
  >
    <SaPageableItems
      ref="pageableItems"
      :page-query="workspacesPageQuery"
      path="workspaces"
      #default="{ item }"
    >
      <WorkspacesOverviewItemPanel :workspace="item" />
    </SaPageableItems>
  </SaOverviewPage>
</template>

<script lang="ts">
  export const WORKSPACES_OVERVIEW_ROUTE_NAME = 'workspaces-overview';
</script>

<script lang="ts" setup>
  import { CREATE_NEW_WORKSPACE_ROUTE_NAME } from '@/pages/settings/workspaces/WorkspaceEditor.vue';
  import SaOverviewPage from '@/components/SaOverviewPage.vue';
  import WorkspacesOverviewItemPanel from '@/pages/settings/workspaces/WorkspacesOverviewItemPanel.vue';
  import { graphql } from '@/services/api/gql';
  import SaPageableItems from '@/components/pageable-items/SaPageableItems.vue';
  import { $t } from '@/services/i18n';

  const workspacesPageQuery = graphql(`
    query workspacesPage($first: Int!, $after: String) {
      workspaces(first: $first, after: $after) {
        edges {
          cursor
          node {
            id
            name
            defaultCurrency
            residency
          }
        }
        pageInfo {
          ...PaginationPageInfo
        }
        totalCount
      }
    }
  `);

</script>
