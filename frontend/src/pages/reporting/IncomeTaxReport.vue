<template>
  <div class="income-tax-report">
    <h2>{{ $t.incomeTaxReport.sections.incomes() }}</h2>
    <IncomeTaxReportTable :data="incomes" />
    <div v-if="report" class="income-tax-report__total text-right">
      <b>{{ $t.incomeTaxReport.sections.total() }}:
        <SaMoneyOutput :currency="defaultCurrency" :amount-in-cents="totalIncomes" />
      </b>
    </div>

    <h2>{{ $t.incomeTaxReport.sections.expenses() }}</h2>
    <IncomeTaxReportTable :data="expenses" />
    <div v-if="report" class="income-tax-report__total text-right">
      <b>{{ $t.incomeTaxReport.sections.total() }}:
        <SaMoneyOutput :currency="defaultCurrency" :amount-in-cents="totalExpenses" />
      </b>
    </div>

    <div v-if="report" class="income-tax-report__net text-right">
      <strong>{{ $t.incomeTaxReport.sections.netTaxableIncome() }}:
        <SaMoneyOutput :currency="defaultCurrency" :amount-in-cents="netTaxableIncome" />
      </strong>
    </div>
  </div>
</template>

<script lang="ts" setup>
  import { computed, onMounted, ref, watch } from 'vue';
  import SaMoneyOutput from '@/components/SaMoneyOutput.vue';
  import IncomeTaxReportTable from '@/pages/reporting/IncomeTaxReportTable.vue';
  import type { IncomeTaxReportItem } from '@/pages/reporting/income-tax-report';
  import { graphql } from '@/services/api/gql';
  import { useLazyQuery } from '@/services/api/use-gql-api';
  import { formatDateToLocalISOString } from '@/services/date-utils';
  import { $t } from '@/services/i18n';
  import { useCurrentWorkspace } from '@/services/workspaces';

  interface SummaryItem {
    category?: { name: string } | null;
    totalAmount: number;
    finalizedCount: number;
  }

  interface Summary {
    items: SummaryItem[];
    totalAmount: number;
    pendingCount: number;
  }

  interface IncomeTaxReportData {
    incomesSummary: Summary;
    expensesSummary: Summary;
  }

  const props = defineProps<{ dateRange: Date[] }>();
  const emit = defineEmits<{(e: 'report-loaded'): void }>();

  const getIncomeTaxReportQuery = useLazyQuery(graphql(`
    query getIncomeTaxReport($workspaceId: String!, $fromDate: LocalDate!, $toDate: LocalDate!) {
      workspace(id: $workspaceId) {
        analytics {
          incomesSummary(fromDate: $fromDate, toDate: $toDate) {
            totalAmount
            pendingCount
            items {
              category { name }
              totalAmount
              finalizedCount
            }
          }
          expensesSummary(fromDate: $fromDate, toDate: $toDate) {
            totalAmount
            pendingCount
            items {
              category { name }
              totalAmount
              finalizedCount
            }
          }
        }
      }
    }
  `), 'workspace');

  const report = ref<IncomeTaxReportData | null>(null);
  const { currentWorkspaceId, defaultCurrency } = useCurrentWorkspace();

  const reloadReport = async () => {
    const workspace = await getIncomeTaxReportQuery({
      workspaceId: currentWorkspaceId,
      fromDate: formatDateToLocalISOString(props.dateRange[0]),
      toDate: formatDateToLocalISOString(props.dateRange[1]),
    });
    report.value = workspace?.analytics ?? null;
    emit('report-loaded');
  };

  const transformSummary = (summary: Summary | undefined): IncomeTaxReportItem[] => {
    if (!summary) return [];

    const items = summary.items
      .filter((item) => item.finalizedCount > 0)
      .map((item) => ({
        categoryName: item.category?.name ?? $t.value.incomeTaxReport.uncategorized(),
        numberOfItems: item.finalizedCount,
        amount: item.totalAmount,
        finalized: true,
      }));

    if (summary.pendingCount > 0) {
      items.push({
        categoryName: $t.value.incomeTaxReport.pending(),
        numberOfItems: summary.pendingCount,
        amount: 0,
        finalized: false,
      });
    }
    return items;
  };

  const incomes = computed(() => transformSummary(report.value?.incomesSummary));
  const expenses = computed(() => transformSummary(report.value?.expensesSummary));
  const totalIncomes = computed(() => report.value?.incomesSummary.totalAmount ?? 0);
  const totalExpenses = computed(() => report.value?.expensesSummary.totalAmount ?? 0);
  const netTaxableIncome = computed(() => totalIncomes.value - totalExpenses.value);

  onMounted(reloadReport);
  watch(() => props.dateRange, reloadReport, { deep: true });
</script>

<style lang="scss" scoped>
  .income-tax-report {
    &__total {
      margin: 16px 0 28px;
    }

    &__net {
      margin-top: 28px;
      font-size: 18px;
    }
  }
</style>
