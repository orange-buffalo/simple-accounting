<template>
  <div class="expense-panel">
    <div class="expense-info">
      <div class="sa-item-title-panel">
        <h3>{{expense.title}}</h3>
        <span class="sa-item-edit-link">
          <svgicon name="pencil"/>
          <el-button type="text"
                     @click="navigateToExpenseEdit">Edit</el-button>
        </span>
      </div>

      <div class="sa-item-attributes">

        <span class="sa-item-attribute">
          <svgicon name="category"/>{{ categoryById(expense.category).name }}
        </span>

        <span class="sa-item-attribute">
          <svgicon name="calendar"/>{{getDatePaid()}}
        </span>

        <span class="sa-item-attribute">
          <svgicon name="banknote"/>
          <money-output :currency="defaultCurrency"
                        :amount="amountInDefaultCurrency()"/>

          <template v-if="isConverted()">
            <money-output :currency="expense.currency"
                          :amount="expense.originalAmount"
                          class="sa-secondary-text"/>
          </template>
        </span>

        <span class="sa-item-attribute"
              v-if="expense.percentOnBusiness < 100">
          <svgicon name="percent"/>Partial: {{expense.percentOnBusiness}}%
        </span>

        <span class="sa-item-attribute"
              v-if="expense.notes">
          <svgicon name="notes"/>
          <span class="sa-clickable" @click="toggleNotes()">Notes provided</span>
        </span>

        <span class="sa-item-attribute"
              v-if="expense.attachments.length">
          <svgicon name="attachment"/>
          <span class="sa-clickable" @click="toggleAttachments()">Attachment provided</span>
        </span>
      </div>

      <div class="sa-item-section" v-if="notesVisible">
        <h4>Notes</h4>
        <!--todo linebreaks-->
        <span class="sa-item-additional-info">{{expense.notes}}</span>
      </div>

      <div class="sa-item-section" v-if="attachmentsVisible">
        <h4>Attachments</h4>
        <span v-for="attachment in attachments"
              :key="attachment.id">
          <document-link :document="attachment"/><br/>
        </span>
      </div>
    </div>

    <div class="expense-amount">
      <div class="amount-value">
        <money-output :currency="totalAmount.currency"
                      :amount="totalAmount.value"/>
      </div>
      <div class="expense-status">
        {{status}}
      </div>
    </div>
  </div>
</template>

<script>
  import {mapGetters, mapState} from 'vuex'
  import MoneyOutput from '@/app/components/MoneyOutput'
  import DocumentLink from '@/app/components/DocumentLink'
  import withMediumDateFormatter from '@/app/components/mixins/with-medium-date-formatter'
  import api from '@/services/api'
  import '@/components/icons/attachment'
  import '@/components/icons/banknote'
  import '@/components/icons/calendar'
  import '@/components/icons/notes'
  import '@/components/icons/category'
  import '@/components/icons/pencil'
  import '@/components/icons/percent'

  export default {
    name: 'ExpenseOverviewPanel',

    mixins: [withMediumDateFormatter],

    components: {
      MoneyOutput,
      DocumentLink
    },

    props: {
      expense: Object
    },

    data: function () {
      return {
        notesVisible: false,
        attachmentsVisible: false,
        attachments: []
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

      status: function () {
        if (this.expense.status === 'FINALIZED') {
          return ''
        } else if (this.expense.status === 'PENDING_CONVERSION') {
          return `Conversion to ${this.defaultCurrency} pending`
        } else {
          return `Waiting for actual rate`
        }
      },

      totalAmount: function () {
        if (this.expense.status === 'FINALIZED') {
          return {
            value: this.expense.reportedAmountInDefaultCurrency,
            currency: this.defaultCurrency
          }
        } else if (this.expense.status === 'PENDING_CONVERSION') {
          return {
            value: this.expense.originalAmount,
            currency: this.expense.currency
          }
        } else {
          return {
            value: this.expense.amountInDefaultCurrency,
            currency: this.defaultCurrency
          }
        }
      }
    },

    methods: {
      amountInDefaultCurrency: function () {
        return this.expense.currency === this.defaultCurrency
            ? this.expense.originalAmount : this.expense.amountInDefaultCurrency
      },

      isConverted: function () {
        return this.expense.currency !== this.defaultCurrency
            && this.expense.amountInDefaultCurrency
      },

      getDatePaid: function () {
        return this.mediumDateFormatter(new Date(this.expense.datePaid))
      },

      toggleNotes: function () {
        this.notesVisible = !this.notesVisible
      },

      toggleAttachments: async function () {
        this.attachmentsVisible = !this.attachmentsVisible

        if (this.attachments.length === 0 && this.expense.attachments && this.expense.attachments.length) {
          let attachments = await api.pageRequest(`/user/workspaces/${this.workspaceId}/documents`)
              .eager()
              .eqFilter("id", this.expense.attachments)
              .getPageData()
          this.attachments = this.attachments.concat(attachments)
        }
      },

      navigateToExpenseEdit: function () {
        this.$router.push({name: 'edit-expense', params: {id: this.expense.id}})
      }
    }
  }
</script>

<style lang="scss">
  @import "@/app/main.scss";

  .expense-panel {
    display: flex;
    justify-content: space-between;
  }

  .expense-info {
    @extend .sa-item-info-panel;
    border-radius: 4px 2px 2px 4px;
    flex-grow: 1;
  }

  .expense-amount {
    @extend .sa-item-info-panel;
    width: 15%;
    border-radius: 2px 4px 4px 2px;
    display: flex;
    flex-flow: column;
    text-align: center;
    justify-content: center;

    .amount-value {
      font-size: 115%;
      font-weight: bolder;
    }
  }
</style>
