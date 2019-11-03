<template>
  <div>
    <!--todo #89: highlight pending items differently-->
    <el-table
      :data="data"
    >
      <el-table-column
        align="left"
        header-align="left"
        prop="tax.title"
        label="Tax"
      />

      <el-table-column
        align="right"
        header-align="right"
        prop="includedItemsNumber"
        label="Number of Items"
      />

      <el-table-column
        align="right"
        header-align="right"
        label="Items Amount"
      >
        <template
          v-if="scope.row.finalized"
          slot-scope="scope"
        >
          <money-output
            :currency="defaultCurrency"
            :amount="scope.row.includedItemsAmount"
          />
        </template>
      </el-table-column>

      <el-table-column
        align="right"
        header-align="right"
        label="Tax Amount"
      >
        <template
          v-if="scope.row.finalized"
          slot-scope="scope"
        >
          <money-output
            :currency="defaultCurrency"
            :amount="scope.row.taxAmount"
          />
        </template>
      </el-table-column>
    </el-table>
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
