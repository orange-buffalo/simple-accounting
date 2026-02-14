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
      <div class="sa-item-info-panel">
        <div class="sa-item-title-panel">
          <h3>{{ category(item).name }}</h3>

          <span class="sa-item-edit-link">
            <!--<pencil-solid-icon/>-->
            <ElButton link>Edit</ElButton>
          </span>
        </div>
        <p>
          {{ category(item).description }}
        </p>
        <p>
          Income: {{ category(item).income }}
        </p>
        <p>
          Expense: {{ category(item).expense }}
        </p>
      </div>
    </SaPageableItems>
  </div>
</template>

<script lang="ts" setup>
import SaPageableItems from '@/components/pageable-items/SaPageableItems.vue';
import SaIcon from '@/components/SaIcon.vue';
import type { ApiPageRequest, CategoryDto, HasOptionalId } from '@/services/api';
import { categoriesApi } from '@/services/api';
import useNavigation from '@/services/use-navigation';
import { useCurrentWorkspace } from '@/services/workspaces';

const { currentWorkspace, currentWorkspaceId } = useCurrentWorkspace();
const categoriesProvider = async (request: ApiPageRequest, config: RequestInit) =>
  categoriesApi.getCategories(
    {
      ...request,
      workspaceId: currentWorkspaceId,
    },
    config,
  );

const { navigateByViewName } = useNavigation();
const navigateToNewCategoryView = () => {
  navigateByViewName('create-new-category');
};

const category = (item: HasOptionalId) => item as CategoryDto;
</script>

<style lang="scss">
  .categories-overview {
    .sa-item-info-panel {
      margin-bottom: 20px;
    }
  }

  .top-buttons-bar {
    margin-bottom: 30px;
    margin-top: -10px;
  }
</style>
