<template>
  <div>
    <div class="sa-page-header">
      <h1>{{ $t.incomesOverview.header() }}</h1>

      <div class="sa-header-options">
        <div>
          <span>{{ $t.incomesOverview.filters.announcement() }}</span>
        </div>

        <div>
          <ElInput
            v-model="freeSearchText"
            :placeholder="$t.incomesOverview.filters.input.placeholder()"
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
          @click="navigateToCreateIncomeView"
        >
          <SaIcon icon="plus-thin" />
          {{ $t.incomesOverview.create() }}
        </ElButton>
      </div>
    </div>

    <SaPageableItems
      #default="{ item: income }"
      :page-query="incomesPageQuery"
      path="workspace.incomes"
      :page-query-arguments="{ workspaceId: currentWorkspaceId, freeSearchText: freeSearchText || null }"
    >
      <IncomesOverviewPanel :income="income" />
    </SaPageableItems>
  </div>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import SaPageableItems from '@/components/pageable-items/SaPageableItems.vue';
  import SaIcon from '@/components/SaIcon.vue';
  import IncomesOverviewPanel from '@/pages/incomes/IncomesOverviewPanel.vue';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import useNavigation from '@/services/use-navigation';
  import { $t } from '@/services/i18n';
  import { graphql } from '@/services/api/gql';

  const incomesPageQuery = graphql(`
    query incomesPage($workspaceId: Long!, $first: Int!, $after: String, $freeSearchText: String) {
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
