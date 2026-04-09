<template>
  <DashboardCard
    header-icon="income"
    :loaded="!loading"
  >
    <template #header>
      <SaMoneyOutput
        class="sa-dashboard__card__header__amount"
        :currency="defaultCurrency"
        :amountInCents="incomes.totalAmount"
      />

      <span class="sa-dashboard__card__header__finalized">{{ $t.dashboard.cards.incomes.totalHeader(incomes.finalizedCount ?? 0) }}</span>

      <span
        v-if="incomes.pendingCount"
        class="sa-dashboard__card__header__pending"
      >{{ $t.dashboard.cards.incomes.pendingHeader(incomes.pendingCount ?? 0) }}</span>
      <span
        v-if="!incomes.pendingCount"
        class="sa-dashboard__card__header__pending"
      >&nbsp;</span>
    </template>

    <template #content>
      <div
        v-for="item in incomes.items"
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

  const getIncomesSummaryQuery = useLazyQuery(graphql(`
    query getIncomesSummary($workspaceId: Long!, $fromDate: LocalDate!, $toDate: LocalDate!) {
      workspace(id: $workspaceId) {
        analytics {
          incomesSummary(fromDate: $fromDate, toDate: $toDate) {
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
    value: maybeIncomes,
  } = useValueLoadedByCurrentWorkspaceAndProp(
    () => props.fromDate && props.toDate,
    async (_, workspaceId) => {
      const workspace = await getIncomesSummaryQuery({
        workspaceId,
        fromDate: props.fromDate.toISOString().slice(0, 10),
        toDate: props.toDate.toISOString().slice(0, 10),
      });
      const summary = workspace?.analytics.incomesSummary;
      if (!summary) return null;
      const sortedItems = [...summary.items].sort((a, b) => b.totalAmount - a.totalAmount);
      return { ...summary, items: sortedItems };
    },
  );
  const incomes = wrapNullable(maybeIncomes);
</script>
