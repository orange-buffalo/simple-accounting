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
        :key="item.category?.name ?? 'unspecified'"
        class="sa-dashboard__card__details__item"
      >
        <span>{{ item.category?.name ?? $t.dashboard.cards.expenses.category.notSpecified() }}</span>
        <SaMoneyOutput
          :currency="defaultCurrency"
          :amount-in-cents="item.totalAmount"
        />
      </div>
    </template>
  </DashboardCard>
</template>

<script lang="ts" setup>
  import { computed } from 'vue';
  import DashboardCard from '@/pages/dashboard/DashboardCard.vue';
  import { wrapNullable } from '@/services/utils';
  import SaMoneyOutput from '@/components/SaMoneyOutput.vue';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import { $t } from '@/services/i18n';

  interface SummaryItem {
    category?: { name: string } | null;
    totalAmount: number;
  }

  interface ExpensesSummaryData {
    totalAmount: number;
    finalizedCount: number;
    pendingCount: number;
    items: SummaryItem[];
  }

  const props = defineProps<{
    loading: boolean,
    summary: ExpensesSummaryData | null,
  }>();

  const { defaultCurrency } = useCurrentWorkspace();

  const expenses = wrapNullable(computed(() => props.summary));
</script>
