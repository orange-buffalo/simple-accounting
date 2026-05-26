<template>
  <SaOverviewPage :header="$t.workspacesOverview.header()">
    <template #actions>
      <ElButton
        round
        @click="navigateToCreateWorkspace"
      >
        <SaIcon icon="plus-thin" />
        {{ $t.workspacesOverview.create() }}
      </ElButton>
    </template>

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

<script lang="ts" setup>
  import SaOverviewPage from '@/components/SaOverviewPage.vue';
  import SaIcon from '@/components/SaIcon.vue';
  import useNavigation from '@/services/use-navigation';
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
