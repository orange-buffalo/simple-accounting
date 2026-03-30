<template>
  <SaOverviewPanelGql
    :header-text="$t.categoriesOverview.header()"
    :create-button-text="$t.categoriesOverview.create()"
    :page-query="categoriesPageQuery"
    path="categories"
    :page-query-arguments="{ workspaceId: currentWorkspaceId }"
    @create="navigateToNewCategoryView"
    #default="{ item }"
  >
    <CategoriesOverviewPanel :category="item" />
  </SaOverviewPanelGql>
</template>

<script lang="ts" setup>
  import SaOverviewPanelGql from '@/components/overview-item/SaOverviewPanelGql.vue';
  import CategoriesOverviewPanel from '@/pages/settings/categories/CategoriesOverviewPanel.vue';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import useNavigation from '@/services/use-navigation';
  import { graphql } from '@/services/api/gql';
  import { $t } from '@/services/i18n';

  const { currentWorkspaceId } = useCurrentWorkspace();

  const categoriesPageQuery = graphql(`
    query categoriesPage($first: Int!, $after: String, $workspaceId: Int!) {
      categories(first: $first, after: $after, workspaceId: $workspaceId) {
        edges {
          cursor
          node {
            id
            name
            description
            income
            expense
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
  const navigateToNewCategoryView = () => {
    navigateByViewName('create-new-category');
  };
</script>
