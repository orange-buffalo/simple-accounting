<template>
  <div>
    <!--todo #89: highlight pending items differently-->
    <ElTable
      :data="data"
    >
      <ElTableColumn
        align="left"
        header-align="left"
        prop="tax.title"
        label="Tax"
      />

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

<script>
  import { withWorkspaces } from '@/components/mixins/with-workspaces';
  import MoneyOutput from '@/components/MoneyOutput';

  export default {
    name: 'TheGeneralTaxReportTable',

    components: {
      MoneyOutput,
    },

    mixins: [withWorkspaces],

    props: {
      data: {},
    },
  };
</script>
