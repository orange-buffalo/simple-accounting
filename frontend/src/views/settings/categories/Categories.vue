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
        <Svgicon name="plus-thin" />
        Add new
      </ElButton>
    </div>

    <DataItems
      :api-path="`/workspaces/${currentWorkspace.id}/categories`"
      :paginator="false"
    >
      <template slot-scope="scope">
        <div class="sa-item-info-panel">
          <div class="sa-item-title-panel">
            <h3>{{ scope.item.name }}</h3>

            <span class="sa-item-edit-link">
              <!--<pencil-solid-icon/>-->
              <ElButton type="text">Edit</ElButton>
            </span>
          </div>
          <p>
            {{ scope.item.description }}
          </p>
          <p>
            Income: {{ scope.item.income }}
          </p>
          <p>
            Expense: {{ scope.item.expense }}
          </p>
        </div>
      </template>
    </DataItems>
  </div>
</template>

<script>
  import '@/components/icons/plus-thin';
  import withWorkspaces from '@/components/mixins/with-workspaces';
  import DataItems from '@/components/DataItems';

  export default {
    name: 'Categories',

    components: {
      DataItems,
    },

    mixins: [withWorkspaces],

    methods: {
      navigateToNewCategoryView() {
        this.$router.push({ name: 'create-new-category' });
      },
    },
  };
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
