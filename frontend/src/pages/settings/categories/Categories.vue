<template>
  <div>
    <div class="sa-page-header">
      <h1>{{ $t.categoriesOverview.header() }}</h1>

      <div class="sa-header-options">
        <div>
          <span>{{ $t.categoriesOverview.filters.announcement() }}</span>
        </div>

        <ElButton
          round
          @click="navigateToNewCategoryView"
        >
          <SaIcon icon="plus-thin" />
          {{ $t.categoriesOverview.create() }}
        </ElButton>
      </div>
    </div>

    <SaPageableItems
      :page-query="categoriesPageQuery"
      path="workspace.categories"
      :page-query-arguments="{ workspaceId: currentWorkspaceId }"
      #default="{ item }"
    >
      <CategoriesOverviewPanel :category="item" />
    </SaPageableItems>
  </div>
</template>

<script lang="ts" setup>
  import SaPageableItems from '@/components/pageable-items/SaPageableItems.vue';
  import SaIcon from '@/components/SaIcon.vue';
  import CategoriesOverviewPanel from '@/pages/settings/categories/CategoriesOverviewPanel.vue';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import useNavigation from '@/services/use-navigation';
  import { graphql } from '@/services/api/gql';
  import { $t } from '@/services/i18n';

  const categoriesPageQuery = graphql(`
    query categoriesPage($workspaceId: String!, $first: Int!, $after: String) {
      workspace(id: $workspaceId) {
        categories(first: $first, after: $after) {
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
    }
  `);

  const { currentWorkspaceId } = useCurrentWorkspace();

  const { navigateByViewName } = useNavigation();
  const navigateToNewCategoryView = () => {
    navigateByViewName('create-new-category');
  };
</script>
