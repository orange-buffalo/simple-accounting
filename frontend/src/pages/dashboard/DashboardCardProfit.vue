<template>
  <DashboardCard
    header-icon="profit"
    :loaded="loaded"
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
  import { ref, watch } from 'vue';
  import DashboardCard from '@/pages/dashboard/DashboardCard.vue';
  import SaMoneyOutput from '@/components/SaMoneyOutput.vue';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import { $t } from '@/services/i18n';
  import { graphql } from '@/services/api/gql';
  import { useLazyQuery } from '@/services/api/use-gql-api.ts';

  const props = defineProps<{
    fromDate: Date,
    toDate: Date,
  }>();

  const {
    defaultCurrency,
    currentWorkspaceId,
  } = useCurrentWorkspace();

  const getProfitSummaryQuery = useLazyQuery(graphql(`
    query getProfitSummary($workspaceId: Long!, $fromDate: LocalDate!, $toDate: LocalDate!) {
      workspace(id: $workspaceId) {
        analytics {
          expensesSummary(fromDate: $fromDate, toDate: $toDate) {
            totalAmount
            currencyExchangeDifference
          }
          incomesSummary(fromDate: $fromDate, toDate: $toDate) {
            totalAmount
            currencyExchangeDifference
          }
          incomeTaxPaymentsSummary(fromDate: $fromDate, toDate: $toDate) {
            totalTaxPayments
          }
        }
      }
    }
  `), 'workspace');

  const loaded = ref(false);
  const incomeTaxableAmount = ref<number>(0);
  const currencyExchangeDifference = ref<number>(0);
  const totalProfit = ref<number>(0);
  const totalTaxPayments = ref<number>(0);

  watch(() => [props.fromDate, props.toDate], async () => {
    loaded.value = false;
    const workspace = await getProfitSummaryQuery({
      workspaceId: currentWorkspaceId,
      fromDate: props.fromDate.toISOString().slice(0, 10),
      toDate: props.toDate.toISOString().slice(0, 10),
    });
    const analytics = workspace?.analytics;
    if (analytics) {
      totalTaxPayments.value = analytics.incomeTaxPaymentsSummary.totalTaxPayments;
      incomeTaxableAmount.value = analytics.incomesSummary.totalAmount - analytics.expensesSummary.totalAmount;
      currencyExchangeDifference.value = analytics.incomesSummary.currencyExchangeDifference
        - analytics.expensesSummary.currencyExchangeDifference;
      totalProfit.value = incomeTaxableAmount.value
        + currencyExchangeDifference.value - analytics.incomeTaxPaymentsSummary.totalTaxPayments;
    }
    loaded.value = true;
  }, { immediate: true });
</script>
