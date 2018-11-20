<template>
  <app-layout>
    <span slot="header"><el-button @click="navigateToCreateExpenseView">Add new</el-button></span>

    <data-items :api-path="`/user/workspaces/${workspaceId}/expenses`"
                :lg="12">
      <template slot-scope="scope">
        <el-card>
          <!--todo add description to expense -->
          <b>Short description</b>
          <br/>
          {{ categoryById(scope.item.category).name }}
          {{getDatePaid(scope.item)}}

          <span>
             <money-output :currency="defaultCurrency"
                           :amount="amountInDefaultCurrency(scope.item)"/>

             <template v-if="isConverted(scope.item)">
               <!--<br/>-->
             <money-output :currency="scope.item.currency"
                           :amount="scope.item.originalAmount"
                           class="secondary-text"/>
             </template>
             </span>

          <template v-if="scope.item.percentOnBusiness < 100">
            Partial: {{scope.item.percentOnBusiness}}%
          </template>

          <money-output :currency="defaultCurrency"
                        :amount="scope.item.reportedAmountInDefaultCurrency"/>

          <br/>


          <el-button type="text"
                     v-if="scope.item.notes"
                     @click="toggleNotes(scope.item)">
            Notes provided
          </el-button>

          <el-button type="text"
                     v-if="scope.item.attachments.length"
                     @click="toggleAttachments(scope.item)">
            Attachment provided
          </el-button>

          <template v-if="isNotesVisible(scope.item)">
            <br/>
            <span>{{scope.item.notes}}</span>
          </template>

          <template v-if="isAttachmentsVisible(scope.item)">
            <br/>
            <span>{{scope.item.attachments}}</span>
          </template>
        </el-card>
      </template>
    </data-items>
  </app-layout>
</template>

<script>
  import DataTable from '@/components/DataTable'
  import DataItems from '@/components/DataItems'
  import {mapState, mapGetters} from 'vuex'
  import MoneyOutput from '@/app/components/MoneyOutput'
  import withMediumDateFormatter from '@/app/components/mixins/with-medium-date-formatter'

  export default {
    name: 'ExpensesOverview',

    mixins: [withMediumDateFormatter],

    components: {
      DataTable,
      MoneyOutput,
      DataItems
    },

    data: function () {
      return {
        notesVisible: [],
        attachmentsVisible: []
      }
    },

    computed: {
      ...mapState({
        workspaceId: state => state.workspaces.currentWorkspace.id,
        defaultCurrency: state => state.workspaces.currentWorkspace.defaultCurrency
      }),

      ...mapGetters({
        categoryById: 'workspaces/categoryById'
      }),

      isNotesVisible: function () {
        return expense => {
          return this.notesVisible[expense.id]
        }
      },

      isAttachmentsVisible: function () {
        return expense => {
          return this.attachmentsVisible[expense.id]
        }
      }
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
      },

      toggleNotes: function (expense) {
        this.$set(this.notesVisible, expense.id, !this.notesVisible[expense.id])
      },

      toggleAttachments: function (expense) {
        this.$set(this.attachmentsVisible, expense.id, !this.attachmentsVisible[expense.id])
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
