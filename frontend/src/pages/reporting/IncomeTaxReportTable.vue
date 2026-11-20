<template>
  <ElTable :data="data">
    <ElTableColumn
      align="left"
      header-align="left"
      :label="$t.incomeTaxReport.tableColumns.category()"
      #default="{ row }"
    >
      <TaxReportRowLabel :name="row.categoryName" :finalized="row.finalized" />
    </ElTableColumn>
    <ElTableColumn
      align="right"
      header-align="right"
      prop="numberOfItems"
      :label="$t.incomeTaxReport.tableColumns.numberOfItems()"
    />
    <ElTableColumn
      align="right"
      header-align="right"
      :label="$t.incomeTaxReport.tableColumns.amount()"
      #default="{ row }"
    >
      <SaMoneyOutput
        v-if="row.finalized"
        :currency="defaultCurrency"
        :amount-in-cents="row.amount"
      />
    </ElTableColumn>
  </ElTable>
</template>

<script lang="ts" setup>
  import SaMoneyOutput from '@/components/SaMoneyOutput.vue';
  import type { IncomeTaxReportItem } from '@/pages/reporting/income-tax-report';
  import TaxReportRowLabel from '@/pages/reporting/TaxReportRowLabel.vue';
  import { $t } from '@/services/i18n';
  import { useCurrentWorkspace } from '@/services/workspaces';

  defineProps<{
    data: IncomeTaxReportItem[];
  }>();

  const { defaultCurrency } = useCurrentWorkspace();
</script>
