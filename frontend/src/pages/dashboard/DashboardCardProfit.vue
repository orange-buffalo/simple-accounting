<template>
  <DashboardCard
    header-icon="profit"
    :loaded="!loading"
  >
    <template #header>
      <SaMoneyOutput
        class="sa-dashboard__card__header__amount"
        :currency="defaultCurrency"
        :amount-in-cents="Math.max(incomeTaxableAmount, 0)"
      />

      <span class="sa-dashboard__card__header__finalized">{{ $t.dashboard.cards.profit.taxableAmount() }}</span>
      <span class="sa-dashboard__card__header__pending">&nbsp;</span>
    </template>

    <template #content>
      <div
        v-if="currencyExchangeDifference"
        class="sa-dashboard__card__details__item"
      >
        <span>{{ $t.dashboard.cards.profit.currencyExchangeDifference() }}</span>
        <SaMoneyOutput
          :currency="defaultCurrency"
          :amount-in-cents="currencyExchangeDifference"
        />
      </div>

      <div class="sa-dashboard__card__details__item">
        <span>{{ $t.dashboard.cards.profit.incomeTaxPayments() }}</span>
        <SaMoneyOutput
          :currency="defaultCurrency"
          :amount-in-cents="totalTaxPayments"
        />
      </div>

      <div class="sa-dashboard__card__details__item">
        <span>{{ $t.dashboard.cards.profit.estimatedTax() }}</span>
        <span>{{ $t.dashboard.cards.profit.estimatedTaxPlaceholder() }}</span>
      </div>

      <div class="sa-dashboard__card__details__item">
        <span>{{ $t.dashboard.cards.profit.profit() }}</span>
        <SaMoneyOutput
          :currency="defaultCurrency"
          :amount-in-cents="totalProfit"
        />
      </div>
    </template>
  </DashboardCard>
</template>

<script lang="ts" setup>
  import DashboardCard from '@/pages/dashboard/DashboardCard.vue';
  import SaMoneyOutput from '@/components/SaMoneyOutput.vue';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import { $t } from '@/services/i18n';

  defineProps<{
    loading: boolean,
    incomeTaxableAmount: number,
    currencyExchangeDifference: number,
    totalTaxPayments: number,
    totalProfit: number,
  }>();

  const { defaultCurrency } = useCurrentWorkspace();
</script>
