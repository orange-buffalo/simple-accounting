<template>
  <SaOverviewPage
    v-model="freeSearchText"
    :header="$t.incomesOverview.header()"
    :filter-placeholder="$t.incomesOverview.filters.input.placeholder()"
  >
    <template #actions>
      <ElButton
        round
        :disabled="!currentWorkspace.editable"
        @click="navigateToCreateIncomeView"
      >
        <SaIcon icon="plus-thin" />
        {{ $t.incomesOverview.create() }}
      </ElButton>
    </template>

    <SaPageableItems
      #default="{ item: income }"
      :page-query="incomesPageQuery"
      path="workspace.incomes"
      :page-query-arguments="{ workspaceId: currentWorkspaceId, freeSearchText: freeSearchText || null }"
    >
      <IncomesOverviewPanel :income="income" />
    </SaPageableItems>
  </SaOverviewPage>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import SaOverviewPage from '@/components/SaOverviewPage.vue';
  import SaPageableItems from '@/components/pageable-items/SaPageableItems.vue';
  import SaIcon from '@/components/SaIcon.vue';
  import IncomesOverviewPanel from '@/pages/incomes/IncomesOverviewPanel.vue';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import useNavigation from '@/services/use-navigation';
  import { $t } from '@/services/i18n';
  import { graphql } from '@/services/api/gql';

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

  const freeSearchText = ref<string | undefined>();

  const { navigateByViewName } = useNavigation();
  const navigateToCreateIncomeView = () => navigateByViewName('create-new-income');
</script>
