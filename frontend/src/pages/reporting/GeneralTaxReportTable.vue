<template>
  <div>
    <!--todo #89: highlight pending items differently-->
    <ElTable :data="data">
      <ElTableColumn
        #default="{ row: item }"
        align="left"
        header-align="left"
        :label="$t.generalTaxReport.tableColumns.tax()"
      >
        <SaGeneralTaxOutput :general-tax-id="item.taxId" />
      </ElTableColumn>

      <ElTableColumn
        align="right"
        header-align="right"
        prop="includedItemsNumber"
        :label="$t.generalTaxReport.tableColumns.numberOfItems()"
        #default="{ row: item }"
      >
        <span v-if="item.finalized">{{ item.includedItemsNumber }}</span>
        <SaStatusLabel v-else simplified status="pending" hide-icon> {{ item.includedItemsNumber }}</SaStatusLabel>
      </ElTableColumn>

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
import SaGeneralTaxOutput from '@/components/general-tax/SaGeneralTaxOutput.vue';
import SaMoneyOutput from '@/components/SaMoneyOutput.vue';
import SaStatusLabel from '@/components/SaStatusLabel.vue';
import type { GeneralTaxReportItem } from '@/pages/reporting/general-tax-report';
import { $t } from '@/services/i18n';
import { useCurrentWorkspace } from '@/services/workspaces';

defineProps<{
  data: GeneralTaxReportItem[];
}>();

const { defaultCurrency } = useCurrentWorkspace();
</script>
