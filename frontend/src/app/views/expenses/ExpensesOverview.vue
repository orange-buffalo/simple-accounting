<template>
  <app-layout>
    <span slot="header"><el-button @click="navigateToCreateExpenseView">Add new</el-button></span>

    <data-items :api-path="`/user/workspaces/${workspaceId}/expenses`">
      <template slot-scope="scope">
        <div class="expense">
          <div class="expense-info">
            <div class="expense-title">
              <b>{{scope.item.title}}</b>
              <span class="edit-expense">
               <pencil-icon/>
              <el-button type="text">Edit</el-button>
            </span>
            </div>

            <div class="expense-attributes">

              <span class="expense-attribute">
                <format-list-bulleted-type-icon/>{{ categoryById(scope.item.category).name }}
              </span>

              <span class="expense-attribute">
                <calendar-import-icon/>{{getDatePaid(scope.item)}}
              </span>

              <span class="expense-attribute">
                <cash-icon/>
                <money-output :currency="defaultCurrency"
                              :amount="amountInDefaultCurrency(scope.item)"/>

                <template v-if="isConverted(scope.item)">
                  <money-output :currency="scope.item.currency"
                                :amount="scope.item.originalAmount"
                                class="secondary-text"/>
                </template>
              </span>

              <span class="expense-attribute"
                    v-if="scope.item.percentOnBusiness < 100">
                <percent-icon/>Partial: {{scope.item.percentOnBusiness}}%
              </span>

              <span class="expense-attribute"
                    v-if="scope.item.notes">
                <message-bulleted-icon/>
                <span class="clickable" @click="toggleNotes(scope.item)">Notes provided</span>
              </span>

              <span class="expense-attribute"
                    v-if="scope.item.attachments.length">
                <paperclip-icon/>
                <span class="clickable" @click="toggleAttachments(scope.item)">Attachment provided</span>
              </span>
            </div>

            <div class="expense-notes" v-if="isNotesVisible(scope.item)">
              <b>Notes</b><br/>
              <!--todo linebreaks-->
              <span>{{scope.item.notes}}</span>
            </div>

            <div class="expense-attachments" v-if="isAttachmentsVisible(scope.item)">
              <b>Attachments</b><br/>
              <span v-for="attachment in expenseAttachments(scope.item)"
                    :key="attachment.id">
                <document-link :document="attachment"/><br/>
              </span>
            </div>
          </div>

          <div class="expense-amount">
            <div class="amount-value">
              <money-output :currency="defaultCurrency"
                            :amount="scope.item.reportedAmountInDefaultCurrency"/>
            </div>
            <div class="expense-status">
              <!--todo calculate status-->
              Finalized
            </div>
          </div>
        </div>
      </template>
    </data-items>
  </app-layout>
</template>

<script>
  import DataItems from '@/components/DataItems'
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
    name: 'ExpensesOverview',

    mixins: [withMediumDateFormatter],

    components: {
      MoneyOutput,
      DataItems,
      DocumentLink,
      CalendarImportIcon,
      FormatListBulletedTypeIcon,
      CashIcon,
      PercentIcon,
      MessageBulletedIcon,
      PaperclipIcon,
      PencilIcon
    },

    data: function () {
      return {
        notesVisible: [],
        attachmentsVisible: [],
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

      isNotesVisible: function () {
        return expense => {
          return this.notesVisible[expense.id]
        }
      },

      isAttachmentsVisible: function () {
        return expense => {
          return this.attachmentsVisible[expense.id]
        }
      },

      expenseAttachments: function () {
        return expense => {
          return expense.attachments.map(attachment => this.attachments[attachment])
              .filter(it => it !== null && typeof it !== 'undefined')
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

      toggleAttachments: async function (expense) {
        this.$set(this.attachmentsVisible, expense.id, !this.attachmentsVisible[expense.id])
        let attachmentsToRequest = expense.attachments.filter(attachment => !this.attachments[attachment])
        if (attachmentsToRequest.length > 0) {
          let attachments = await api.pageRequest(`/user/workspaces/${this.workspaceId}/documents`)
              .eager()
              .eqFilter("id", attachmentsToRequest)
              .getPageData()
          attachments.forEach(document => this.$set(this.attachments, document.id, document))
        }
      }
    }
  }
</script>

<style lang="scss">
  .secondary-text {
    font-size: 90%;
    color: #8e8e8e;
  }

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
  }

</style>
