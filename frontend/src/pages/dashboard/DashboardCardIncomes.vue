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

      <span class="sa-dashboard__card__header__finalized">{{ $t.dashboard.cards.incomes.totalHeader(incomes.finalizedCount) }}</span>

      <span
        v-if="incomes.pendingCount"
        class="sa-dashboard__card__header__pending"
      >{{ $t.dashboard.cards.incomes.pendingHeader(incomes.pendingCount) }}</span>
      <span
        v-if="!incomes.pendingCount"
        class="sa-dashboard__card__header__pending"
      >&nbsp;</span>
    </template>

    <template #content>
      <div
        v-for="item in incomes.items"
        :key="item.categoryId || 'fake'"
        class="sa-dashboard__card__details__item"
      >
        <span><SaCategoryOutput
          :category-id="item.categoryId"
          :unspecified-category="!item.categoryId"
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
  import { statisticsApi } from '@/services/api';
  import SaMoneyOutput from '@/components/SaMoneyOutput.vue';
  import SaCategoryOutput from '@/components/category/SaCategoryOutput.vue';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import { $t } from '@/services/i18n';
  import { formatDateToLocalISOString } from '@/services/date-utils';

  const props = defineProps<{
    fromDate: Date,
    toDate: Date,
  }>();

  const { defaultCurrency } = useCurrentWorkspace();

  const {
    loading,
    value: maybeIncomes,
  } = useValueLoadedByCurrentWorkspaceAndProp(
    () => props.fromDate && props.toDate,
    async (_, workspaceId) => {
      const response = await statisticsApi.getIncomesStatistics({
        workspaceId,
        fromDate: formatDateToLocalISOString(props.fromDate),
        toDate: formatDateToLocalISOString(props.toDate),
      });
      response.items.sort((a, b) => b.totalAmount - a.totalAmount);
      return response;
    },
  );
  const incomes = wrapNullable(maybeIncomes);
</script>
