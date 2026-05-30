<template>
  <SaOverviewPage
    v-model="overviewFilters"
    :header="$t.categoriesOverview.header()"
    :filters="categoryFilters"
    :create-action-label="$t.categoriesOverview.create()"
    create-action-view-name="create-new-category"
  >
    <SaPageableItems
      :page-query="categoriesPageQuery"
      path="workspace.categories"
      :page-query-arguments="{
        workspaceId: currentWorkspaceId,
        freeSearchText: overviewFilters.freeSearchText,
        typeIn: overviewFilters.typeIn,
      }"
      #default="{ item }"
    >
      <CategoriesOverviewPanel :category="item" />
    </SaPageableItems>
  </SaOverviewPage>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import SaOverviewPage from '@/components/SaOverviewPage.vue';
  import SaPageableItems from '@/components/pageable-items/SaPageableItems.vue';
  import CategoriesOverviewPanel from '@/pages/settings/categories/CategoriesOverviewPanel.vue';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import { graphql } from '@/services/api/gql';
  import { $t } from '@/services/i18n';
  import { CategoryType } from '@/services/api/gql/schema-types';
  import {
    createOverviewFilters,
    type SaOverviewFilterConfigs,
    type SaOverviewFilters,
  } from '@/components/overview-page/overview-page-filters';

  type CategoriesOverviewFilters = SaOverviewFilters & {
    freeSearchText: string | null,
    typeIn: CategoryType[] | null,
  };

  const categoriesPageQuery = graphql(`
    query categoriesPage(
      $workspaceId: String!,
      $first: Int!,
      $after: String,
      $freeSearchText: String,
      $typeIn: [CategoryType!]
    ) {
      workspace(id: $workspaceId) {
        categories(first: $first, after: $after, freeSearchText: $freeSearchText, typeIn: $typeIn) {
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
  const overviewFilters = ref(createOverviewFilters<CategoriesOverviewFilters>({
    freeSearchText: null,
    typeIn: null,
  }));
  const categoryFilters: SaOverviewFilterConfigs<CategoriesOverviewFilters> = {
    freeSearchText: {
      type: 'text',
      label: $t.value.categoriesOverview.filters.freeSearchText.label(),
    },
    typeIn: {
      type: 'multi-select',
      label: $t.value.categoriesOverview.filters.type.label(),
      options: [
        {
          label: $t.value.categoriesOverview.type.income(),
          value: CategoryType.Income,
        },
        {
          label: $t.value.categoriesOverview.type.expense(),
          value: CategoryType.Expense,
        },
      ],
    },
  };

</script>
