<template>
  <SaOverviewPage v-model="overviewFilters" :header="$t.accountStatements.header()" :filters="filters">
    <SaPageableItems
      :page-query="statementsQuery"
      path="workspace.accountStatements"
      :page-query-arguments="queryArguments"
      #default="{ item }"
    >
      <AccountStatementPanel :statement="item" />
    </SaPageableItems>
  </SaOverviewPage>
</template>

<script lang="ts" setup>
  import { computed, ref } from 'vue';
  import SaOverviewPage from '@/components/SaOverviewPage.vue';
  import SaPageableItems from '@/components/pageable-items/SaPageableItems.vue';
  import AccountStatementPanel from './AccountStatementPanel.vue';
  import {
    createOverviewFilters, type SaOverviewFilterConfigs,
  } from '@/components/overview-page/overview-page-filters';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import { graphql } from '@/services/api/gql';
  import type { IntegrationProvider } from '@/services/api/gql/graphql';
  import { $t } from '@/services/i18n';

  type StatementFilters = {
    freeSearchText: string | null,
    providerIn: IntegrationProvider[] | null,
    dateRange: [string, string] | null,
  };
  const { currentWorkspaceId } = useCurrentWorkspace();
  const overviewFilters = ref(createOverviewFilters<StatementFilters>({
    freeSearchText: null, providerIn: null, dateRange: null,
  }));
  const filters = computed(() => ({
    freeSearchText: { type: 'text', label: $t.value.accountStatements.search() },
    providerIn: {
      type: 'multi-select', label: $t.value.accountStatements.provider(),
      options: [{ value: 'WISE', label: $t.value.integrations.wise.name() }],
    },
    dateRange: { type: 'date-range', label: $t.value.accountStatements.dateRange() },
  } satisfies SaOverviewFilterConfigs<StatementFilters>));
  const localDayStart = (date: string, dayOffset = 0) => {
    const [year, month, day] = date.split('-').map(Number);
    return new Date(year, month - 1, day + dayOffset).toISOString();
  };
  const queryArguments = computed(() => ({
    workspaceId: currentWorkspaceId,
    freeSearchText: overviewFilters.value.freeSearchText,
    providerIn: overviewFilters.value.providerIn,
    occurredAtFrom: overviewFilters.value.dateRange ? localDayStart(overviewFilters.value.dateRange[0]) : null,
    occurredAtBefore: overviewFilters.value.dateRange ? localDayStart(overviewFilters.value.dateRange[1], 1) : null,
  }));
  const statementsQuery = graphql(`
    query accountStatementsPage(
      $workspaceId: String!, $first: Int!, $after: String, $freeSearchText: String,
      $providerIn: [IntegrationProvider!], $occurredAtFrom: DateTime, $occurredAtBefore: DateTime
    ) {
      workspace(id: $workspaceId) {
        accountStatements(first: $first, after: $after, freeSearchText: $freeSearchText,
          providerIn: $providerIn, occurredAtFrom: $occurredAtFrom, occurredAtBefore: $occurredAtBefore) {
          edges {
            cursor
            node {
              id provider accountId accountCurrency externalId type occurredAt
              amountInCents currency partyName feesInCents feesCurrency runningBalanceInCents
              runningBalanceCurrency additionalData originalAmountInCents originalCurrency
              sourceAmountInCents sourceCurrency targetAmountInCents targetCurrency exchangeRate createdAt
              linkedInvoice { id invoiceNumber }
            }
          }
          pageInfo { ...PaginationPageInfo }
          totalCount
        }
      }
    }
  `);
</script>
