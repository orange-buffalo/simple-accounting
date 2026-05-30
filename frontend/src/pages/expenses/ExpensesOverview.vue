<template>
  <SaOverviewPage
    v-model="overviewFilters"
    :header="$t.expensesOverview.header()"
    :filters="expenseFilters"
    :create-action-label="$t.expensesOverview.create()"
    create-action-view-name="create-new-expense"
    :create-action-disabled="!currentWorkspace.editable"
  >
    <SaPageableItems
      #default="{ item: expense }"
      :page-query="expensesPageQuery"
      path="workspace.expenses"
      :page-query-arguments="{ workspaceId: currentWorkspaceId, freeSearchText: overviewFilters.freeSearchText }"
    >
      <ExpensesOverviewPanel :expense="expense" />
    </SaPageableItems>
  </SaOverviewPage>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import SaOverviewPage from '@/components/SaOverviewPage.vue';
  import ExpensesOverviewPanel from '@/pages/expenses/ExpensesOverviewPanel.vue';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import SaPageableItems from '@/components/pageable-items/SaPageableItems.vue';
  import { $t } from '@/services/i18n/i18n-services';
  import { graphql } from '@/services/api/gql';
  import {
    createOverviewFilters,
    type SaOverviewFilterConfigs,
    type SaOverviewFilters,
  } from '@/components/overview-page/overview-page-filters';

  type ExpensesOverviewFilters = SaOverviewFilters & {
    freeSearchText: string | null,
  };

  const expensesPageQuery = graphql(`
    query expensesPage($workspaceId: String!, $first: Int!, $after: String, $freeSearchText: String) {
      workspace(id: $workspaceId) {
        expenses(first: $first, after: $after, freeSearchText: $freeSearchText) {
          edges {
            cursor
            node {
              id
              version
              title
              datePaid
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
              percentOnBusiness
              notes
              status
              generalTax {
                title
              }
              generalTaxRateInBps
              generalTaxAmount
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

  const overviewFilters = ref(createOverviewFilters<ExpensesOverviewFilters>({
    freeSearchText: null,
  }));
  const expenseFilters: SaOverviewFilterConfigs<ExpensesOverviewFilters> = {
    freeSearchText: {
      type: 'text',
      label: $t.value.expensesOverview.filters.freeSearchText.label(),
    },
  };
</script>
