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

    <DataItems
      :api-path="`/workspaces/${currentWorkspace.id}/categories`"
      :paginator="false"
      #default="{item}"
    >
        <div class="sa-item-info-panel">
          <div class="sa-item-title-panel">
            <h3>{{ item.name }}</h3>

            <span class="sa-item-edit-link">
              <!--<pencil-solid-icon/>-->
              <ElButton type="text">Edit</ElButton>
            </span>
          </div>
          <p>
            {{ item.description }}
          </p>
          <p>
            Income: {{ item.income }}
          </p>
          <p>
            Expense: {{ item.expense }}
          </p>
        </div>
    </DataItems>
  </div>
</template>

<script lang="ts">
  import { defineComponent } from '@vue/composition-api';
  import DataItems from '@/components/DataItems';
  import SaIcon from '@/components/SaIcon';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import useNavigation from '@/components/navigation/useNavigation';

  export default defineComponent({
    components: {
      SaIcon,
      DataItems,
    },

    setup() {
      const { currentWorkspace } = useCurrentWorkspace();
      const { navigateByViewName } = useNavigation();

      const navigateToNewCategoryView = () => {
        navigateByViewName('create-new-category');
      };

      return {
        currentWorkspace,
        navigateToNewCategoryView,
      };
    },
  });
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
