<template>
  <div class="categories-overview">
    <div class="sa-page-header">
      <h1>Categories</h1>
    </div>

    <div class="top-buttons-bar">
      <ElButton
        round
        @click="navigateToNewCategoryView"
      >
        <SaIcon icon="plus-thin" />
        Add new
      </ElButton>
    </div>

    <SaPageableItemsGql
      :page-query="categoriesPageQuery"
      path="workspace.categories"
      :page-query-arguments="{ workspaceId: currentWorkspaceId }"
      #default="{ item }"
    >
      <CategoriesOverviewPanel :category="item" />
    </SaPageableItemsGql>
  </div>
</template>

<script lang="ts" setup>
  import SaPageableItemsGql from '@/components/pageable-items/SaPageableItemsGql.vue';
  import SaIcon from '@/components/SaIcon.vue';
  import CategoriesOverviewPanel from '@/pages/settings/categories/CategoriesOverviewPanel.vue';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import useNavigation from '@/services/use-navigation';
  import { graphql } from '@/services/api/gql';

  const categoriesPageQuery = graphql(`
    query categoriesPage($workspaceId: Long!, $first: Int!, $after: String) {
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

<style lang="scss">
  .categories-overview {
    .top-buttons-bar {
      margin-bottom: 30px;
      margin-top: -10px;
    }
  }
</style>
