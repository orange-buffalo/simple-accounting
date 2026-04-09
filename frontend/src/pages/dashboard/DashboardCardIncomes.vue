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
  import { computed } from 'vue';
  import DashboardCard from '@/pages/dashboard/DashboardCard.vue';
  import { wrapNullable } from '@/services/utils';
  import SaMoneyOutput from '@/components/SaMoneyOutput.vue';
  import SaCategoryOutput from '@/components/category/SaCategoryOutput.vue';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import { $t } from '@/services/i18n';

  interface SummaryItem {
    category?: { id: number } | null;
    totalAmount: number;
  }

  interface IncomesSummaryData {
    totalAmount: number;
    finalizedCount: number;
    pendingCount: number;
    items: SummaryItem[];
  }

  const props = defineProps<{
    loading: boolean,
    summary: IncomesSummaryData | null,
  }>();

  const { defaultCurrency } = useCurrentWorkspace();

  const incomes = wrapNullable(computed(() => props.summary));
</script>
