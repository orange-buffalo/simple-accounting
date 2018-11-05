<template>
  <app-layout>
    <span slot="header"><el-button @click="navigateToCreateExpenseView">Add new</el-button></span>
    
    <el-card>
    <data-table stripe :api-path="`/user/workspaces/${workspaceId}/expenses`">
      <el-table-column
          label="Category">
        <template slot-scope="scope">
          {{ categoryById(scope.row.category).name }}
        </template>
      </el-table-column>
      <el-table-column
          prop="currency"
          label="currency">
      </el-table-column>
      <el-table-column
          prop="originalAmount"
          label="originalAmount">
      </el-table-column>
      <el-table-column
          prop="amountInDefaultCurrency"
          label="amountInDefaultCurrency">
      </el-table-column>
      <el-table-column
          prop="actualAmountInDefaultCurrency"
          label="actualAmountInDefaultCurrency">
      </el-table-column>
      <el-table-column
          prop="percentOnBusinessInBps"
          label="percentOnBusinessInBps">
      </el-table-column>
      <el-table-column
          prop="notes"
          label="notes">
      </el-table-column>

    </data-table>
      </el-card>
  </app-layout>
</template>

<script>
  import DataTable from '@/components/DataTable'
  import {mapState, mapGetters} from 'vuex'

  export default {
    name: 'ExpensesOverview',
    components: {
      DataTable
    },
    computed: {
      ...mapState({
        workspaceId: state => state.workspaces.currentWorkspace.id
      }),
      ...mapGetters({
        categoryById: 'workspaces/categoryById'
      })
    } ,
    methods: {
      navigateToCreateExpenseView: function () {
        this.$router.push({name: 'create-new-expense'})
      }
    }
  }
</script>
