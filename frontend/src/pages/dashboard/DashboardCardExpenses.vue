<template>
  <DashboardCard
    header-icon="expense"
    :loaded="!loading"
  >
    <template #header>
      <SaMoneyOutput
        class="sa-dashboard__card__header__amount"
        :currency="defaultCurrency"
        :amountInCents="expenses.totalAmount"
      />

      <span class="sa-dashboard__card__header__finalized">{{ $t.dashboard.cards.expenses.totalHeader(expenses.finalizedCount ?? 0) }}</span>

      <span
        v-if="expenses.pendingCount"
        class="sa-dashboard__card__header__pending"
      >{{ $t.dashboard.cards.expenses.pendingHeader(expenses.pendingCount ?? 0) }}</span>
      <span
        v-if="!expenses.pendingCount"
        class="sa-dashboard__card__header__pending"
      >&nbsp;</span>
    </template>

    <template #content>
      <div
        v-for="item in expenses.items"
        :key="item.category?.id ?? 'fake'"
        class="sa-dashboard__card__details__item"
      >
        <span><SaCategoryOutput
          :category-id="item.category?.id"
          :unspecified-category="!item.category"
        /></span>
        <SaMoneyOutput
          :currency="defaultCurrency"
          :amount-in-cents="item.totalAmount"
        />
      </div>
    </template>
  </DashboardCard>
</template>

<script lang="ts" setup>
  import DashboardCard from '@/pages/dashboard/DashboardCard.vue';
  import { useValueLoadedByCurrentWorkspaceAndProp, wrapNullable } from '@/services/utils';
  import SaMoneyOutput from '@/components/SaMoneyOutput.vue';
  import SaCategoryOutput from '@/components/category/SaCategoryOutput.vue';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import { $t } from '@/services/i18n';
  import { graphql } from '@/services/api/gql';
  import { useLazyQuery } from '@/services/api/use-gql-api.ts';

  const props = defineProps<{
    fromDate: Date,
    toDate: Date,
  }>();

  const { defaultCurrency } = useCurrentWorkspace();

  const getExpensesSummaryQuery = useLazyQuery(graphql(`
    query getExpensesSummary($workspaceId: Long!, $fromDate: LocalDate!, $toDate: LocalDate!) {
      workspace(id: $workspaceId) {
        analytics {
          expensesSummary(fromDate: $fromDate, toDate: $toDate) {
            totalAmount
            finalizedCount
            pendingCount
            items {
              category {
                id
              }
              totalAmount
            }
          }
        }
      }
    }
  `), 'workspace');

  const {
    loading,
    value: maybeExpenses,
  } = useValueLoadedByCurrentWorkspaceAndProp(
    () => props.fromDate && props.toDate,
    async (_, workspaceId) => {
      const workspace = await getExpensesSummaryQuery({
        workspaceId,
        fromDate: props.fromDate.toISOString().slice(0, 10),
        toDate: props.toDate.toISOString().slice(0, 10),
      });
      const summary = workspace?.analytics.expensesSummary;
      if (!summary) return null;
      const sortedItems = [...summary.items].sort((a, b) => b.totalAmount - a.totalAmount);
      return { ...summary, items: sortedItems };
    },
  );
  const expenses = wrapNullable(maybeExpenses);
</script>
