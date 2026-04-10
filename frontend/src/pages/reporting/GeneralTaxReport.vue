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
  import { useCurrentWorkspace } from '@/services/workspaces';
  import type { GeneralTaxReportItem } from '@/pages/reporting/general-tax-report';
  import { $t } from '@/services/i18n';
  import { formatDateToLocalISOString } from '@/services/date-utils';
  import { graphql } from '@/services/api/gql';
  import { useLazyQuery } from '@/services/api/use-gql-api';

  interface FinalizedTax {
    tax: { title: string };
    taxAmount: number;
    includedItemsNumber: number;
    includedItemsAmount: number;
  }

  interface PendingTax {
    tax: { title: string };
    includedItemsNumber: number;
  }

  function transformTaxes(finalizedTaxes: FinalizedTax[], pendingTaxes: PendingTax[]) {
    let taxes: GeneralTaxReportItem[] = finalizedTaxes.map((item) => ({
      taxTitle: item.tax.title,
      taxAmount: item.taxAmount,
      includedItemsAmount: item.includedItemsAmount,
      includedItemsNumber: item.includedItemsNumber,
      finalized: true,
    }));

    taxes = taxes.concat(pendingTaxes.map((item) => ({
      taxTitle: item.tax.title,
      taxAmount: 0,
      includedItemsAmount: 0,
      includedItemsNumber: item.includedItemsNumber,
      finalized: false,
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

  const getGeneralTaxReportQuery = useLazyQuery(graphql(`
    query getGeneralTaxReport($workspaceId: Long!, $fromDate: LocalDate!, $toDate: LocalDate!) {
      workspace(id: $workspaceId) {
        analytics {
          generalTaxesSummary(fromDate: $fromDate, toDate: $toDate) {
            finalizedCollectedTaxes {
              tax { title }
              taxAmount
              includedItemsNumber
              includedItemsAmount
            }
            finalizedPaidTaxes {
              tax { title }
              taxAmount
              includedItemsNumber
              includedItemsAmount
            }
            pendingCollectedTaxes {
              tax { title }
              includedItemsNumber
            }
            pendingPaidTaxes {
              tax { title }
              includedItemsNumber
            }
          }
        }
      }
    }
  `), 'workspace');

  interface GeneralTaxesSummaryData {
    finalizedCollectedTaxes: FinalizedTax[];
    finalizedPaidTaxes: FinalizedTax[];
    pendingCollectedTaxes: PendingTax[];
    pendingPaidTaxes: PendingTax[];
  }

  const report = ref<GeneralTaxesSummaryData | null>(null);

  const {
    defaultCurrency,
    currentWorkspaceId,
  } = useCurrentWorkspace();

  const reloadReport = async () => {
    const workspace = await getGeneralTaxReportQuery({
      workspaceId: currentWorkspaceId,
      fromDate: formatDateToLocalISOString(props.dateRange[0]),
      toDate: formatDateToLocalISOString(props.dateRange[1]),
    });
    report.value = workspace?.analytics?.generalTaxesSummary ?? null;
    emit('report-loaded');
  };

  onMounted(reloadReport);

  watch(() => props.dateRange, reloadReport, { deep: true });

  const collectedTaxes = computed(() => (report.value !== null
    ? transformTaxes(report.value.finalizedCollectedTaxes, report.value.pendingCollectedTaxes)
    : []));

  const paidTaxes = computed(() => (report.value !== null
    ? transformTaxes(report.value.finalizedPaidTaxes, report.value.pendingPaidTaxes)
    : []));

  const totalCollectedAmount = computed(() => getTotalAmount(collectedTaxes.value));
  const totalPaidAmount = computed(() => getTotalAmount(paidTaxes.value));
</script>
