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

    <SaPageableItems
      :page-provider="categoriesProvider"
      :api-path="`/workspaces/${currentWorkspace.id}/categories`"
      #default="{ item }"
    >
      <CategoriesOverviewPanel :category="item as CategoryDto" />
    </SaPageableItems>
  </div>
</template>

<script lang="ts" setup>
  import SaPageableItems from '@/components/pageable-items/SaPageableItems.vue';
  import SaIcon from '@/components/SaIcon.vue';
  import CategoriesOverviewPanel from '@/pages/settings/categories/CategoriesOverviewPanel.vue';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import useNavigation from '@/services/use-navigation';
  import type { ApiPageRequest, CategoryDto } from '@/services/api';
  import { categoriesApi } from '@/services/api';

  const {
    currentWorkspace,
    currentWorkspaceId,
  } = useCurrentWorkspace();
  const categoriesProvider = async (request: ApiPageRequest, config: RequestInit) => categoriesApi.getCategories({
    ...request,
    workspaceId: currentWorkspaceId,
  }, config);

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
