<template>
  <div>
    <h2>{{ $t.generalTaxReport.sections.collected() }}</h2>
    <GeneralTaxReportTable :data="collectedTaxes" />
    <div
      v-if="report"
      class="text-right"
    >
      <br>
      <b>{{ $t.generalTaxReport.sections.total() }}:
        <SaMoneyOutput
          :currency="defaultCurrency"
          :amount-in-cents="totalCollectedAmount"
        />
      </b>
    </div>
    <br>

    <h2>{{ $t.generalTaxReport.sections.paid() }}</h2>
    <GeneralTaxReportTable :data="paidTaxes" />
    <div
      v-if="report"
      class="text-right"
    >
      <br>
      <b>{{ $t.generalTaxReport.sections.total() }}:
        <SaMoneyOutput
          :currency="defaultCurrency"
          :amount-in-cents="totalPaidAmount"
        />
      </b>
    </div>
  </div>
</template>

<script lang="ts" setup>
  import {
    computed, onMounted, ref, watch,
  } from 'vue';
  import SaMoneyOutput from '@/components/SaMoneyOutput.vue';
  import GeneralTaxReportTable from '@/pages/reporting/GeneralTaxReportTable.vue';
  import type {
    FinalizedTaxSummaryItemDto,
    GeneralTaxReportDto,
    PendingTaxSummaryItemDto,
  } from '@/services/api';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import type { GeneralTaxReportItem } from '@/pages/reporting/general-tax-report';
  import { reportingApi } from '@/services/api';
  import { $t } from '@/services/i18n';
  import { formatDateToLocalISOString } from '@/services/date-utils';

  function transformTaxes(finalizedTaxes: FinalizedTaxSummaryItemDto[], pendingTaxes: PendingTaxSummaryItemDto[]) {
    let taxes: GeneralTaxReportItem[] = finalizedTaxes.map((tax) => ({
      ...tax,
      finalized: true,
      taxId: tax.tax,
    }));

    taxes = taxes.concat(pendingTaxes.map((tax) => ({
      ...tax,
      taxAmount: 0,
      includedItemsAmount: 0,
      finalized: false,
      taxId: tax.tax,
    })));

    return taxes;
  }

  function getTotalAmount(taxes: GeneralTaxReportItem[]) {
    return taxes.map((tax) => tax.taxAmount)
      .reduce((it, sum) => sum + it, 0);
  }

  const props = defineProps<{
    dateRange: Date[],
  }>();

  const emit = defineEmits<{(e: 'report-loaded'): void }>();

  const report = ref<GeneralTaxReportDto | undefined>();

  const {
    defaultCurrency,
    currentWorkspaceId,
  } = useCurrentWorkspace();

  const reloadReport = async () => {
    report.value = await reportingApi.getGeneralTaxReport({
      workspaceId: currentWorkspaceId,
      fromDate: formatDateToLocalISOString(props.dateRange[0]),
      toDate: formatDateToLocalISOString(props.dateRange[1]),
    });
    emit('report-loaded');
  };

  onMounted(reloadReport);

  watch(() => props.dateRange, reloadReport, { deep: true });

  const collectedTaxes = computed(() => (report.value !== undefined
    ? transformTaxes(report.value.finalizedCollectedTaxes, report.value.pendingCollectedTaxes)
    : []));

  const paidTaxes = computed(() => (report.value !== undefined
    ? transformTaxes(report.value.finalizedPaidTaxes, report.value.pendingPaidTaxes)
    : []));

  const totalCollectedAmount = computed(() => getTotalAmount(collectedTaxes.value));
  const totalPaidAmount = computed(() => getTotalAmount(paidTaxes.value));
</script>
