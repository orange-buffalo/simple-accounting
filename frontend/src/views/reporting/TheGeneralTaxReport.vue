<template>
  <div>
    <h2>Collected</h2>
    <TheGeneralTaxReportTable :data="collectedTaxes" />
    <div
      v-if="report"
      class="text-right"
    >
      <br>
      <b>Total:
        <MoneyOutput
          :currency="defaultCurrency"
          :amount="totalCollectedAmount"
        />
      </b>
    </div>
    <br>

    <h2>Paid</h2>
    <TheGeneralTaxReportTable :data="paidTaxes" />
    <div
      v-if="report"
      class="text-right"
    >
      <br>
      <b>Total:
        <MoneyOutput
          :currency="defaultCurrency"
          :amount="totalPaidAmount"
        />
      </b>
    </div>
  </div>
</template>

<script lang="ts">
  import MoneyOutput from '@/components/MoneyOutput';
  import TheGeneralTaxReportTable from '@/views/reporting/TheGeneralTaxReportTable';
  import {
    computed,
    defineComponent, onMounted, PropType, ref, watch,
  } from '@vue/composition-api';
  import {
    apiClient,
    apiDateString,
    FinalizedTaxSummaryItemDto,
    GeneralTaxReportDto,
    PendingTaxSummaryItemDto,
  } from '@/services/api';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import { ReportTax } from '@/views/reporting/general-tax-report';

  function transformTaxes(finalizedTaxes: FinalizedTaxSummaryItemDto[], pendingTaxes: PendingTaxSummaryItemDto[]) {
    let taxes: ReportTax[] = finalizedTaxes.map((tax) => ({
      ...tax,
      finalized: true,
      taxId: tax.tax,
    }));

    taxes = taxes.concat(pendingTaxes.map((tax) => ({
      taxAmount: 0,
      includedItemsAmount: 0,
      finalized: false,
      taxId: tax.tax,
    })));

    return taxes;
  }

  function getTotalAmount(taxes: ReportTax[]) {
    return taxes.map((tax) => tax.taxAmount)
      .reduce((it, sum) => sum + it, 0);
  }

  export default defineComponent({
    components: {
      TheGeneralTaxReportTable,
      MoneyOutput,
    },

    props: {
      dateRange: {
        type: Array as PropType<Date[]>,
      },
    },

    setup(props, { emit }) {
      const report = ref<GeneralTaxReportDto | null>(null);

      const {
        defaultCurrency,
        currentWorkspaceId,
      } = useCurrentWorkspace();

      const reloadReport = async () => {
        const response = await apiClient.getGeneralTaxReport({
          workspaceId: currentWorkspaceId,
          fromDate: apiDateString(props.dateRange![0]),
          toDate: apiDateString(props.dateRange![1]),
        });
        report.value = response.data;
        emit('report-loaded');
      };

      onMounted(reloadReport);

      watch(() => props.dateRange, reloadReport, { deep: true });

      const collectedTaxes = computed(() => (report.value != null
        ? transformTaxes(report.value.finalizedCollectedTaxes, report.value.pendingCollectedTaxes)
        : []));

      const paidTaxes = computed(() => (report.value != null
        ? transformTaxes(report.value.finalizedPaidTaxes, report.value.pendingPaidTaxes)
        : []));

      const totalCollectedAmount = computed(() => getTotalAmount(collectedTaxes.value));
      const totalPaidAmount = computed(() => getTotalAmount(paidTaxes.value));

      return {
        defaultCurrency,
        collectedTaxes,
        paidTaxes,
        totalCollectedAmount,
        totalPaidAmount,
        report,
      };
    },
  });
</script>
