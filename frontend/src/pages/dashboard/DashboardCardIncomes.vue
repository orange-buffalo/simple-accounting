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

      <!--      TODO #459: translations -->
      <span class="sa-dashboard__card__header__finalized">Total of {{ incomes.finalizedCount }} incomes</span>

      <span
        v-if="incomes.pendingCount"
        class="sa-dashboard__card__header__pending"
      >Pending {{ incomes.pendingCount }} more</span>
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
        fromDate: props.fromDate,
        toDate: props.toDate,
      });
      response.items.sort((a, b) => b.totalAmount - a.totalAmount);
      return response;
    },
  );
  const incomes = wrapNullable(maybeIncomes);
</script>
