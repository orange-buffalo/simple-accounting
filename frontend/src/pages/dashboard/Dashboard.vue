<template>
  <SaPage class="sa-dashboard" :header="$t.dashboard.header()">
    <template #header-options>
      <!--suppress HtmlDeprecatedAttribute -->
      <ElDatePicker
        v-model="selectedDateRange"
        type="daterange"
        :format="datePickerFormat"
        align="right"
        unlink-panels
        :range-separator="$t.dashboard.dateRange.separator()"
        :start-placeholder="$t.dashboard.dateRange.startPlaceholder()"
        :end-placeholder="$t.dashboard.dateRange.endPlaceholder()"
      />
    </template>

    <div class="sa-dashboard__row">
      <DashboardCardExpenses
        :loading="loading"
        :summary="expensesSummary"
      />
      <DashboardCardIncomes
        :loading="loading"
        :summary="incomesSummary"
      />
      <DashboardCardProfit
        :loading="loading"
        :income-taxable-amount="profitData.incomeTaxableAmount"
        :currency-exchange-difference="profitData.currencyExchangeDifference"
        :total-tax-payments="profitData.totalTaxPayments"
        :income-tax-estimate="profitData.incomeTaxEstimate"
        :total-profit="profitData.totalProfit"
      />
      <DashboardInvoices
        :invoices="invoicesData"
      />
    </div>
  </SaPage>
</template>

<script lang="ts" setup>
  import { ref, computed, watch } from 'vue';
  import SaPage from '@/components/SaPage.vue';
  import { useStorage } from '@/services/storage';
  import DashboardCardExpenses from '@/pages/dashboard/DashboardCardExpenses.vue';
  import DashboardCardIncomes from '@/pages/dashboard/DashboardCardIncomes.vue';
  import DashboardCardProfit from '@/pages/dashboard/DashboardCardProfit.vue';
  import DashboardInvoices from '@/pages/dashboard/DashboardInvoices.vue';
  import { $t } from '@/services/i18n';
  import { graphql } from '@/services/api/gql';
  import { useLazyQuery } from '@/services/api/use-gql-api.ts';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import type { GetDashboardAnalyticsQuery } from '@/services/api/gql/graphql';
  import { datePickerFormat } from '@/components/date-picker/date-picker-localization';
  import { formatDateToLocalISOString } from '@/services/date-utils';

  type DashboardInvoiceNode = GetDashboardAnalyticsQuery['workspace']['invoices']['edges'][0]['node'];

  const storage = useStorage<Array<Date>>('dashboard.selected-date-range');

  const selectedDateRange = ref<Array<Date>>(storage.getOrDefault([]));
  if (!selectedDateRange.value.length) {
    const now = new Date();
    selectedDateRange.value = [
      new Date(now.getFullYear(), 0, 1),
      now,
    ];
  } else {
    selectedDateRange.value = selectedDateRange.value.map((it) => new Date(it));
  }

  watch(selectedDateRange, (newDatesRanges) => {
    storage.set(newDatesRanges);
  });

  const selectedFromDate = computed(() => (selectedDateRange.value[0]));
  const selectedToDate = computed(() => (selectedDateRange.value[1]));

  const { currentWorkspaceId } = useCurrentWorkspace();

  const getDashboardAnalyticsQuery = useLazyQuery(graphql(`
    query getDashboardAnalytics($workspaceId: String!, $fromDate: LocalDate!, $toDate: LocalDate!) {
      workspace(id: $workspaceId) {
        analytics {
          expensesSummary(fromDate: $fromDate, toDate: $toDate) {
            totalAmount
            finalizedCount
            pendingCount
            currencyExchangeDifference
            items {
              category {
                name
              }
              totalAmount
            }
          }
          incomesSummary(fromDate: $fromDate, toDate: $toDate) {
            totalAmount
            finalizedCount
            pendingCount
            currencyExchangeDifference
            items {
              category {
                name
              }
              totalAmount
            }
          }
          incomeTaxPaymentsSummary(fromDate: $fromDate, toDate: $toDate) {
            totalTaxPayments
          }
          incomeTaxEstimate(fromDate: $fromDate, toDate: $toDate) {
            amount
            unavailableReason
            countryCode
            workspaceCurrency
            taxCurrency
          }
        }
        invoices(first: 100, statusIn: [SENT, OVERDUE]) {
          edges {
            node {
              id
              title
              amount
              currency
              dateIssued
              dateSent
              dueDate
              status
              customer {
                name
              }
            }
          }
        }
      }
    }
  `), 'workspace');

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

  interface IncomesSummaryData {
    totalAmount: number;
    finalizedCount: number;
    pendingCount: number;
    items: SummaryItem[];
  }

  interface ProfitData {
    incomeTaxableAmount: number;
    currencyExchangeDifference: number;
    totalTaxPayments: number;
    incomeTaxEstimate: GetDashboardAnalyticsQuery['workspace']['analytics']['incomeTaxEstimate'] | null;
    totalProfit: number;
  }

  const loading = ref(true);
  const expensesSummary = ref<ExpensesSummaryData | null>(null);
  const incomesSummary = ref<IncomesSummaryData | null>(null);
  const invoicesData = ref<DashboardInvoiceNode[]>([]);
  const profitData = ref<ProfitData>({
    incomeTaxableAmount: 0,
    currencyExchangeDifference: 0,
    totalTaxPayments: 0,
    incomeTaxEstimate: null,
    totalProfit: 0,
  });

  watch([selectedFromDate, selectedToDate], async ([fromDate, toDate]) => {
    if (!fromDate || !toDate) return;
    loading.value = true;
    const workspace = await getDashboardAnalyticsQuery({
      workspaceId: currentWorkspaceId,
      fromDate: formatDateToLocalISOString(fromDate),
      toDate: formatDateToLocalISOString(toDate),
    });
    const analytics = workspace?.analytics;
    if (analytics) {
      expensesSummary.value = {
        ...analytics.expensesSummary,
        items: [...analytics.expensesSummary.items].sort((a, b) => b.totalAmount - a.totalAmount),
      };
      incomesSummary.value = {
        ...analytics.incomesSummary,
        items: [...analytics.incomesSummary.items].sort((a, b) => b.totalAmount - a.totalAmount),
      };
      const incomeTaxableAmount = analytics.incomesSummary.totalAmount - analytics.expensesSummary.totalAmount;
      const currencyExchangeDifference = analytics.incomesSummary.currencyExchangeDifference
        - analytics.expensesSummary.currencyExchangeDifference;
      profitData.value = {
        incomeTaxableAmount,
        currencyExchangeDifference,
        totalTaxPayments: analytics.incomeTaxPaymentsSummary.totalTaxPayments,
        incomeTaxEstimate: analytics.incomeTaxEstimate,
        totalProfit: incomeTaxableAmount + currencyExchangeDifference
          - (analytics.incomeTaxEstimate.amount ?? 0),
      };
    } else {
      expensesSummary.value = null;
      incomesSummary.value = null;
    }
    invoicesData.value = workspace?.invoices.edges.map(e => e.node) ?? [];
    loading.value = false;
  }, { immediate: true });
</script>

<style lang="scss">
  @use "@/styles/vars.scss" as *;

  .sa-dashboard {
    .sa-page__header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      flex-wrap: wrap;
      gap: 16px;
      margin-bottom: 28px;

      h1 {
        margin: 0;
        font-size: 28px;
        letter-spacing: -0.5px;
      }

      &:after {
        display: none;
      }
    }

    .sa-page__header-options {
      min-width: 0;
      max-width: 100%;
    }

    .el-date-editor {
      max-width: 100%;
      box-sizing: border-box;
    }
  }

  .sa-dashboard__row {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 24px;
    align-items: stretch;

    @media (max-width: 1050px) {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    @media (max-width: 700px) {
      grid-template-columns: minmax(0, 1fr);
    }
  }
</style>
