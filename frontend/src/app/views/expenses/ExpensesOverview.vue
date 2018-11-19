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

        <el-table-column label="Amount"
                         header-align="right"
                         align="right">
          <div class="money-output"
               slot-scope="scope">
            <money-output :currency="defaultCurrency"
                          :amount="amountInDefaultCurrency(scope.row)"/>
            <template v-if="isConverted(scope.row)">
              <br/>
              <money-output :currency="scope.row.currency"
                            :amount="scope.row.originalAmount"
                            class="secondary-text"/>
            </template>
          </div>
        </el-table-column>

        <el-table-column label="Reported Amount"
                         header-align="right"
                         align="right">
          <div class="money-output"
               slot-scope="scope">
            <!--todo reportedAmountInDefaultCurrency -->
            <money-output :currency="defaultCurrency"
                          :amount="scope.row.reportedAmountInDefaultCurrency"/>
            <template v-if="scope.row.percentOnBusiness < 100">
              <br/>
              <!--todo format number as per locale -->
              <div class="secondary-text">
                <span>{{scope.row.percentOnBusiness}}% of </span>
                <money-output :currency="defaultCurrency"
                              :amount="scope.row.amountInDefaultCurrency"/>
              </div>
            </template>
          </div>
        </el-table-column>

        <el-table-column
            label="Date"
            header-align="center"
            align="center">
          <span slot-scope="scope">{{getDatePaid(scope.row)}}</span>
        </el-table-column>

        <!--todo links to the attachments, probably asynchronously-->

      </data-table>
    </el-card>
  </app-layout>
</template>

<script>
  import DataTable from '@/components/DataTable'
  import {mapState, mapGetters} from 'vuex'
  import MoneyOutput from '@/app/components/MoneyOutput'
  import withMediumDateFormatter from '@/app/components/mixins/with-medium-date-formatter'

  export default {
    name: 'ExpensesOverview',

    mixins: [withMediumDateFormatter],

    components: {
      DataTable,
      MoneyOutput
    },

    computed: {
      ...mapState({
        workspaceId: state => state.workspaces.currentWorkspace.id,
        defaultCurrency: state => state.workspaces.currentWorkspace.defaultCurrency
      }),

      ...mapGetters({
        categoryById: 'workspaces/categoryById'
      })
    },

    methods: {
      navigateToCreateExpenseView: function () {
        this.$router.push({name: 'create-new-expense'})
      },

      amountInDefaultCurrency: function (expense) {
        return expense.currency === this.defaultCurrency ? expense.originalAmount : expense.amountInDefaultCurrency
      },

      isConverted: function (expense) {
        return expense.currency !== this.defaultCurrency
            && expense.amountInDefaultCurrency
      },

      getDatePaid: function (expense) {
        return this.mediumDateFormatter(new Date(expense.datePaid))
      }
    }
  }
</script>

<style lang="scss">
  .secondary-text {
    font-size: 90%;
    color: #8e8e8e;
  }
</style>
