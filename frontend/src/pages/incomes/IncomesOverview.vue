<template>
  <SaOverviewPage
    v-model="overviewFilters"
    :header="$t.incomesOverview.header()"
    :filter-placeholder="$t.incomesOverview.filters.input.placeholder()"
    :create-action-label="$t.incomesOverview.create()"
    create-action-view-name="create-new-income"
    :create-action-disabled="!currentWorkspace.editable"
  >
    <SaPageableItems
      #default="{ item: income }"
      :page-query="incomesPageQuery"
      path="workspace.incomes"
      :page-query-arguments="{ workspaceId: currentWorkspaceId, freeSearchText: overviewFilters.freeSearchText }"
    >
      <IncomesOverviewPanel :income="income" />
    </SaPageableItems>
  </SaOverviewPage>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import SaOverviewPage from '@/components/SaOverviewPage.vue';
  import SaPageableItems from '@/components/pageable-items/SaPageableItems.vue';
  import IncomesOverviewPanel from '@/pages/incomes/IncomesOverviewPanel.vue';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import { $t } from '@/services/i18n';
  import { graphql } from '@/services/api/gql';
  import { createOverviewFilters } from '@/components/overview-page/overview-page-filters';

  const incomesPageQuery = graphql(`
    query incomesPage($workspaceId: String!, $first: Int!, $after: String, $freeSearchText: String) {
      workspace(id: $workspaceId) {
        incomes(first: $first, after: $after, freeSearchText: $freeSearchText) {
          edges {
            cursor
            node {
              id
              version
              title
              dateReceived
              currency
              originalAmount
              convertedAmounts {
                originalAmountInDefaultCurrency
                adjustedAmountInDefaultCurrency
              }
              useDifferentExchangeRateForIncomeTaxPurposes
              incomeTaxableAmounts {
                originalAmountInDefaultCurrency
                adjustedAmountInDefaultCurrency
              }
              notes
              status
              generalTax {
                title
              }
              generalTaxRateInBps
              generalTaxAmount
              linkedInvoice {
                id
                title
              }
              category {
                name
              }
              attachments {
                ...DocumentData
              }
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

  const { currentWorkspaceId, currentWorkspace } = useCurrentWorkspace();

  const overviewFilters = ref(createOverviewFilters());
</script>
