<template>
  <div>
    <!--todo #89: highlight pending items differently-->
    <ElTable
      :data="data"
    >
      <ElTableColumn
        v-slot="{row: item}"
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
      />

      <ElTableColumn
        align="right"
        header-align="right"
        label="Items Amount"
      >
        <template
          v-if="scope.row.finalized"
          slot-scope="scope"
        >
          <MoneyOutput
            :currency="defaultCurrency"
            :amount="scope.row.includedItemsAmount"
          />
        </template>
      </ElTableColumn>

      <ElTableColumn
        align="right"
        header-align="right"
        label="Tax Amount"
      >
        <template
          v-if="scope.row.finalized"
          slot-scope="scope"
        >
          <MoneyOutput
            :currency="defaultCurrency"
            :amount="scope.row.taxAmount"
          />
        </template>
      </ElTableColumn>
    </ElTable>
  </div>
</template>

<script lang="ts">
  import MoneyOutput from '@/components/MoneyOutput';
  import SaGeneralTaxOutput from '@/components/general-tax/SaGeneralTaxOutput';
  import { defineComponent, PropType } from '@vue/composition-api';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import { ReportTax } from '@/views/reporting/general-tax-report';

  export default defineComponent({
    components: {
      SaGeneralTaxOutput,
      MoneyOutput,
    },

    props: {
      data: {
        type: Array as PropType<ReportTax[]>,
        required: true,
      },
    },

    setup() {
      const { defaultCurrency } = useCurrentWorkspace();
      return {
        defaultCurrency,
      };
    },
  });
</script>
