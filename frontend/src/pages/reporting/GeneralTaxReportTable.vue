<template>
  <div>
    <ElTable :data="data">
      <ElTableColumn
        #default="{ row: item }"
        align="left"
        header-align="left"
        :label="$t.generalTaxReport.tableColumns.tax()"
      >
        <TaxReportRowLabel :name="item.taxTitle" :finalized="item.finalized" />
      </ElTableColumn>

      <ElTableColumn
        align="right"
        header-align="right"
        prop="includedItemsNumber"
        :label="$t.generalTaxReport.tableColumns.numberOfItems()"
      />

      <ElTableColumn
        align="right"
        header-align="right"
        :label="$t.generalTaxReport.tableColumns.itemsAmount()"
        #default="{ row }"
      >
        <SaMoneyOutput
          v-if="row.finalized"
          :currency="defaultCurrency"
          :amount-in-cents="row.includedItemsAmount"
        />
      </ElTableColumn>

      <ElTableColumn
        align="right"
        header-align="right"
        :label="$t.generalTaxReport.tableColumns.taxAmount()"
        #default="{ row }"
      >
        <SaMoneyOutput
          v-if="row.finalized"
          :currency="defaultCurrency"
          :amount-in-cents="row.taxAmount"
        />
      </ElTableColumn>
    </ElTable>
  </div>
</template>

<script lang="ts" setup>
  import SaMoneyOutput from '@/components/SaMoneyOutput.vue';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import type { GeneralTaxReportItem } from '@/pages/reporting/general-tax-report';
  import TaxReportRowLabel from '@/pages/reporting/TaxReportRowLabel.vue';
  import { $t } from '@/services/i18n';

  defineProps<{
    data: GeneralTaxReportItem[]
  }>();

  const { defaultCurrency } = useCurrentWorkspace();
</script>
