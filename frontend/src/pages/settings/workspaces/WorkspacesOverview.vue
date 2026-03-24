<template>
  <div>
    <div class="sa-page-header">
      <h1>Workspaces</h1>

      <div class="sa-header-options">
        <span />

        <ElButton
          round
          @click="navigateToCreateWorkspace"
        >
          <SaIcon icon="plus-thin" />
          Create new workspace
        </ElButton>
      </div>
    </div>

    <SaPageableItemsGql
      ref="pageableItems"
      :page-query="workspacesPageQuery"
      path="workspaces"
      #default="{ item }"
    >
      <WorkspacesOverviewItemPanel :workspace="item" />
    </SaPageableItemsGql>
  </div>
</template>

<script lang="ts" setup>
  import SaIcon from '@/components/SaIcon.vue';
  import useNavigation from '@/services/use-navigation';
  import WorkspacesOverviewItemPanel from '@/pages/settings/workspaces/WorkspacesOverviewItemPanel.vue';
  import { graphql } from '@/services/api/gql';
  import SaPageableItemsGql from '@/components/pageable-items/SaPageableItemsGql.vue';

  const workspacesPageQuery = graphql(`
    query workspacesPage($first: Int!, $after: String) {
      workspaces(first: $first, after: $after) {
        edges {
          cursor
          node {
            id
            name
            defaultCurrency
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
  const navigateToCreateWorkspace = () => navigateByViewName('create-new-workspace');
</script>
