<template>
  <ElTable :data="data">
    <ElTableColumn
      align="left"
      header-align="left"
      prop="categoryName"
      :label="$t.incomeTaxReport.tableColumns.category()"
    />
    <ElTableColumn
      align="right"
      header-align="right"
      :label="$t.incomeTaxReport.tableColumns.numberOfItems()"
      #default="{ row }"
    >
      <span v-if="row.finalized">{{ row.numberOfItems }}</span>
      <SaStatusLabel v-else simplified status="pending" hide-icon>
        {{ row.numberOfItems }}
      </SaStatusLabel>
    </ElTableColumn>
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
  import SaStatusLabel from '@/components/SaStatusLabel.vue';
  import type { IncomeTaxReportItem } from '@/pages/reporting/income-tax-report';
  import { $t } from '@/services/i18n';
  import { useCurrentWorkspace } from '@/services/workspaces';

  defineProps<{
    data: IncomeTaxReportItem[];
  }>();

  const { defaultCurrency } = useCurrentWorkspace();
</script>
