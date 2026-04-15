<template>
  <div>
    <div class="sa-page-header">
      <h1>{{ $t.expensesOverview.header() }}</h1>

      <div class="sa-header-options">
        <div>
          <span>{{ $t.expensesOverview.filters.announcement() }}</span>
        </div>

        <div>
          <ElInput
            v-model="freeSearchText"
            :placeholder="$t.expensesOverview.filters.input.placeholder()"
            clearable
          >
            <template #prefix>
              <i class="el-icon-search el-input__icon" />
            </template>
          </ElInput>
        </div>

        <ElButton
          round
          :disabled="!currentWorkspace.editable"
          @click="navigateToCreateExpenseView"
        >
          <SaIcon icon="plus-thin" />
          {{ $t.expensesOverview.create() }}
        </ElButton>
      </div>
    </div>

    <SaPageableItemsGql
      #default="{ item: expense }"
      :page-query="expensesPageQuery"
      path="workspace.expenses"
      :page-query-arguments="{ workspaceId: currentWorkspaceId, freeSearchText: freeSearchText || null }"
    >
      <ExpensesOverviewPanel :expense="expense" />
    </SaPageableItemsGql>
  </div>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import SaIcon from '@/components/SaIcon.vue';
  import ExpensesOverviewPanel from '@/pages/expenses/ExpensesOverviewPanel.vue';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import SaPageableItemsGql from '@/components/pageable-items/SaPageableItemsGql.vue';
  import useNavigation from '@/services/use-navigation';
  import { $t } from '@/services/i18n/i18n-services';
  import { graphql } from '@/services/api/gql';

  const expensesPageQuery = graphql(`
    query expensesPage($workspaceId: Long!, $first: Int!, $after: String, $freeSearchText: String) {
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
              generalTaxId
              generalTaxRateInBps
              generalTaxAmount
              category {
                id
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
