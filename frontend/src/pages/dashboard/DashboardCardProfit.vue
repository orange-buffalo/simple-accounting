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
        <SaMoneyOutput
          v-if="incomeTaxEstimate?.amount != null"
          :currency="defaultCurrency"
          :amount-in-cents="incomeTaxEstimate.amount"
        />
        <ElTooltip
          v-else-if="incomeTaxEstimate?.unavailableReason"
          :content="unavailableMessage"
          placement="top-end"
        >
          <span class="sa-dashboard__tax-unavailable">
            {{ $t.dashboard.cards.profit.estimatedTaxUnavailable() }}
            <SaIcon icon="warning-circle" :size="14" />
          </span>
        </ElTooltip>
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
  import SaIcon from '@/components/SaIcon.vue';
  import { ElTooltip } from 'element-plus';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import { $t } from '@/services/i18n';
  import { getCountryName } from '@/services/i18n/countries';
  import type { GetDashboardAnalyticsQuery } from '@/services/api/gql/graphql';
  import { computed } from 'vue';

  const props = defineProps<{
    loading: boolean,
    incomeTaxableAmount: number,
    currencyExchangeDifference: number,
    totalTaxPayments: number,
    incomeTaxEstimate: GetDashboardAnalyticsQuery['workspace']['analytics']['incomeTaxEstimate'] | null,
    totalProfit: number,
  }>();

  const { defaultCurrency } = useCurrentWorkspace();

  const unavailableMessage = computed(() => {
    const estimate = props.incomeTaxEstimate;
    if (!estimate) return '';
    switch (estimate.unavailableReason) {
    case 'PARTIAL_YEAR':
      return $t.value.dashboard.cards.profit.estimateRequiresFullYear();
    case 'NO_TAX_RATES':
      return $t.value.dashboard.cards.profit.estimateNoRates(getCountryName(estimate.countryCode));
    case 'CURRENCY_MISMATCH':
      return $t.value.dashboard.cards.profit.estimateCurrencyMismatch(
        estimate.workspaceCurrency, estimate.taxCurrency ?? '',
      );
    default:
      return '';
    }
  });
</script>

<style scoped lang="scss">
  .sa-dashboard__tax-unavailable {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    cursor: help;
  }
</style>
