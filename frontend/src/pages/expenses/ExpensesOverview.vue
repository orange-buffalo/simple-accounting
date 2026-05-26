<template>
  <SaOverviewPage
    v-model="freeSearchText"
    :header="$t.expensesOverview.header()"
    :announcement="$t.expensesOverview.filters.announcement()"
    :filter-placeholder="$t.expensesOverview.filters.input.placeholder()"
  >
    <template #actions>
      <ElButton
        round
        :disabled="!currentWorkspace.editable"
        @click="navigateToCreateExpenseView"
      >
        <SaIcon icon="plus-thin" />
        {{ $t.expensesOverview.create() }}
      </ElButton>
    </template>

    <SaPageableItems
      #default="{ item: expense }"
      :page-query="expensesPageQuery"
      path="workspace.expenses"
      :page-query-arguments="{ workspaceId: currentWorkspaceId, freeSearchText: freeSearchText || null }"
    >
      <ExpensesOverviewPanel :expense="expense" />
    </SaPageableItems>
  </SaOverviewPage>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import SaOverviewPage from '@/components/SaOverviewPage.vue';
  import SaIcon from '@/components/SaIcon.vue';
  import ExpensesOverviewPanel from '@/pages/expenses/ExpensesOverviewPanel.vue';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import SaPageableItems from '@/components/pageable-items/SaPageableItems.vue';
  import useNavigation from '@/services/use-navigation';
  import { $t } from '@/services/i18n/i18n-services';
  import { graphql } from '@/services/api/gql';

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

  const freeSearchText = ref<string | undefined>();

  const { navigateByViewName } = useNavigation();
  const navigateToCreateExpenseView = () => navigateByViewName('create-new-expense');
</script>
