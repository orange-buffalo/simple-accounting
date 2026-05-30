<template>
  <SaOverviewPage
    v-model="overviewFilters"
    :header="$t.generalTaxesOverview.header()"
    :filter-placeholder="$t.generalTaxesOverview.filters.input.placeholder()"
    :create-action-label="$t.generalTaxesOverview.create()"
    create-action-view-name="create-new-general-tax"
  >
    <SaPageableItems
      :page-query="generalTaxesPageQuery"
      path="workspace.generalTaxes"
      :page-query-arguments="{ workspaceId: currentWorkspaceId, freeSearchText: overviewFilters.freeSearchText }"
      #default="{ item }"
    >
      <GeneralTaxOverviewPanel :tax="item" />
    </SaPageableItems>
  </SaOverviewPage>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import SaOverviewPage from '@/components/SaOverviewPage.vue';
  import SaPageableItems from '@/components/pageable-items/SaPageableItems.vue';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import GeneralTaxOverviewPanel from '@/pages/settings/general-taxes/GeneralTaxOverviewPanel.vue';
  import { graphql } from '@/services/api/gql';
  import { $t } from '@/services/i18n';
  import { createOverviewFilters } from '@/components/overview-page/overview-page-filters';

  const generalTaxesPageQuery = graphql(`
    query generalTaxesPage($workspaceId: String!, $first: Int!, $after: String, $freeSearchText: String) {
      workspace(id: $workspaceId) {
        generalTaxes(first: $first, after: $after, freeSearchText: $freeSearchText) {
          edges {
            cursor
            node {
              id
              title
              description
              rateInBps
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

  const overviewFilters = ref(createOverviewFilters());
</script>
