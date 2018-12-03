<template>
  <div class="expense">
    <div class="expense-info">
      <div class="expense-title">
        <b>{{expense.title}}</b>
        <span class="edit-expense">
               <pencil-icon/>
              <el-button type="text">Edit</el-button>
            </span>
      </div>

      <div class="expense-attributes">

              <span class="expense-attribute">
                <format-list-bulleted-type-icon/>{{ categoryById(expense.category).name }}
              </span>

        <span class="expense-attribute">
                <calendar-import-icon/>{{getDatePaid()}}
              </span>

        <span class="expense-attribute">
                <cash-icon/>
                <money-output :currency="defaultCurrency"
                              :amount="amountInDefaultCurrency()"/>

                <template v-if="isConverted()">
                  <money-output :currency="expense.currency"
                                :amount="expense.originalAmount"
                                class="secondary-text"/>
                </template>
              </span>

        <span class="expense-attribute"
              v-if="expense.percentOnBusiness < 100">
                <percent-icon/>Partial: {{expense.percentOnBusiness}}%
              </span>

        <span class="expense-attribute"
              v-if="expense.notes">
                <message-bulleted-icon/>
                <span class="clickable" @click="toggleNotes()">Notes provided</span>
              </span>

        <span class="expense-attribute"
              v-if="expense.attachments.length">
                <paperclip-icon/>
                <span class="clickable" @click="toggleAttachments()">Attachment provided</span>
              </span>
      </div>

      <div class="expense-notes" v-if="notesVisible">
        <b>Notes</b><br/>
        <!--todo linebreaks-->
        <span>{{expense.notes}}</span>
      </div>

      <div class="expense-attachments" v-if="attachmentsVisible">
        <b>Attachments</b><br/>
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
  import CalendarImportIcon from "vue-material-design-icons/CalendarImport"
  import FormatListBulletedTypeIcon from "vue-material-design-icons/FormatListBulletedType"
  import CashIcon from "vue-material-design-icons/Cash"
  import PercentIcon from "vue-material-design-icons/Percent"
  import MessageBulletedIcon from "vue-material-design-icons/MessageBulleted"
  import PaperclipIcon from "vue-material-design-icons/Paperclip"
  import PencilIcon from "vue-material-design-icons/Pencil"

  export default {
    name: 'ExpenseOverviewPanel',

    mixins: [withMediumDateFormatter],

    components: {
      MoneyOutput,
      DocumentLink,
      CalendarImportIcon,
      FormatListBulletedTypeIcon,
      CashIcon,
      PercentIcon,
      MessageBulletedIcon,
      PaperclipIcon,
      PencilIcon
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
      }
    }
  }
</script>

<style lang="scss">
  .expense {
    display: flex;
    justify-content: space-between;

    .expense-info {
      padding: 20px;
      border: 1px solid #ebeef5;
      background-color: #fff;
      border-radius: 4px 2px 2px 4px;
      overflow: hidden;
      flex-grow: 1;

      .expense-title {
        display: flex;
        justify-content: space-between;

        b {
          font-size: 130%;
        }

        .edit-expense {
          color: #409EFF;

          .material-design-icon {
            margin-right: 3px;
            font-size: 90%;
          }
        }
      }

      .expense-attributes {

        .expense-attribute {
          display: inline-block;
          margin-right: 30px;
          margin-top: 15px;

          .material-design-icon {
            margin-right: 5px;
            font-size: 120%;
          }

          .secondary-text {
            margin-left: 5px;
          }

          .clickable {
            cursor: pointer;
          }
        }
      }

      .expense-notes {
        margin-top: 20px;

        b {
          font-size: 105%;
        }

        span {
          font-style: italic;
        }
      }

      .expense-attachments {
        margin-top: 20px;

        b {
          font-size: 105%;
        }
      }
    }

    .expense-amount {
      padding: 20px;
      width: 15%;
      border: 1px solid #ebeef5;
      background-color: #fff;
      border-radius: 2px 4px 4px 2px;
      overflow: hidden;
      display: flex;
      flex-flow: column;
      text-align: center;
      justify-content: center;

      .amount-value {
        font-size: 115%;
        font-weight: bolder;
      }
    }

    .secondary-text {
      font-size: 90%;
      color: #8e8e8e;
    }
  }
</style>
