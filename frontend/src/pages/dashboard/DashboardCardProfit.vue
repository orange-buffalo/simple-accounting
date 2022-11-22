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

      <!--      TODO #459: translations -->
      <span class="sa-dashboard__card__header__finalized">Taxable Amount</span>
      <span class="sa-dashboard__card__header__pending">&nbsp;</span>
    </template>

    <template #content>
      <div
        v-if="currencyExchangeDifference"
        class="sa-dashboard__card__details__item"
      >
        <span>Currency exchange rate difference</span>
        <SaMoneyOutput
          :currency="defaultCurrency"
          :amount-in-cents="currencyExchangeDifference"
        />
      </div>

      <div class="sa-dashboard__card__details__item">
        <span>Income Tax Payments</span>
        <SaMoneyOutput
          :currency="defaultCurrency"
          :amount-in-cents="totalTaxPayments"
        />
      </div>

      <div class="sa-dashboard__card__details__item">
        <span>Estimated Tax</span>
        <span>coming soon..</span>
      </div>

      <div class="sa-dashboard__card__details__item">
        <span>Profit</span>
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
  import { statisticsApi } from '@/services/api';
  import SaMoneyOutput from '@/components/SaMoneyOutput.vue';
  import { useCurrentWorkspace } from '@/services/workspaces';

  const props = defineProps<{
    fromDate: Date,
    toDate: Date,
  }>();

  const {
    defaultCurrency,
    currentWorkspaceId,
  } = useCurrentWorkspace();

  const loaded = ref(false);
  const incomeTaxableAmount = ref<number>(0);
  const currencyExchangeDifference = ref<number>(0);
  const totalProfit = ref<number>(0);
  const totalTaxPayments = ref<number>(0);

  let abortController: AbortController | null = null;
  watch(() => [props.fromDate, props.toDate], async () => {
    loaded.value = false;
    if (abortController !== null) {
      abortController.abort();
    }
    abortController = new AbortController();
    const [expenses, incomes, incomeTaxPayments] = await Promise.all([
      statisticsApi.getExpensesStatistics({
        workspaceId: currentWorkspaceId,
        fromDate: props.fromDate,
        toDate: props.toDate,
      }, {
        signal: abortController.signal,
      }),
      statisticsApi.getIncomesStatistics({
        workspaceId: currentWorkspaceId,
        fromDate: props.fromDate,
        toDate: props.toDate,
      }, {
        signal: abortController.signal,
      }),
      statisticsApi.getTaxPaymentsStatistics({
        workspaceId: currentWorkspaceId,
        fromDate: props.fromDate,
        toDate: props.toDate,
      }, {
        signal: abortController.signal,
      }),
    ]);

    totalTaxPayments.value = incomeTaxPayments.totalTaxPayments;
    incomeTaxableAmount.value = incomes.totalAmount - expenses.totalAmount;
    currencyExchangeDifference.value = incomes.currencyExchangeDifference - expenses.currencyExchangeDifference;
    totalProfit.value = incomeTaxableAmount.value
      + currencyExchangeDifference.value - incomeTaxPayments.totalTaxPayments;

    loaded.value = true;
  }, { immediate: true });
</script>
