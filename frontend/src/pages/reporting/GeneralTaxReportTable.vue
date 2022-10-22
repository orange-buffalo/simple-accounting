<template>
  <!--  TODO: translations-->
  <div>
    <!--todo #89: highlight pending items differently-->
    <ElTable :data="data">
      <ElTableColumn
        #default="{ row: item }"
        align="left"
        header-align="left"
        label="Tax"
      >
        <SaGeneralTaxOutput :general-tax-id="item.taxId" />
      </ElTableColumn>

      <ElTableColumn
        align="right"
        header-align="right"
        prop="includedItemsNumber"
        label="Number of Items"
        #default="{ row: item }"
      >
        <span v-if="item.finalized">{{ item.includedItemsNumber }}</span>
        <SaStatusLabel v-else simplified status="pending" hide-icon> {{ item.includedItemsNumber }}</SaStatusLabel>
      </ElTableColumn>

      <ElTableColumn
        align="right"
        header-align="right"
        label="Items Amount"
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
        label="Tax Amount"
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
  import SaGeneralTaxOutput from '@/components/general-tax/SaGeneralTaxOutput.vue';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import type { GeneralTaxReportItem } from '@/pages/reporting/general-tax-report';
  import SaStatusLabel from '@/components/SaStatusLabel.vue';

  defineProps<{
    data: GeneralTaxReportItem[]
  }>();

  const { defaultCurrency } = useCurrentWorkspace();
</script>
