<template>
  <div class="income">
    <div class="income-info">
      <div class="income-title">
        <b>{{income.title}}</b>
        <span class="edit-income">
          <pencil-icon/>
          <el-button type="text"
                     @click="navigateToIncomeEdit">Edit</el-button>
        </span>
      </div>

      <div class="income-attributes">

        <span class="income-attribute">
          <format-list-bulleted-type-icon/>{{ categoryById(income.category).name }}
        </span>

        <span class="income-attribute">
          <calendar-import-icon/>{{getDateReceived()}}
        </span>

        <span class="income-attribute">
          <cash-icon/>
          <money-output :currency="defaultCurrency"
                        :amount="amountInDefaultCurrency()"/>

          <template v-if="isConverted()">
            <money-output :currency="income.currency"
                          :amount="income.originalAmount"
                          class="secondary-text"/>
          </template>
        </span>

        <span class="income-attribute"
              v-if="income.notes">
          <message-bulleted-icon/>
          <span class="clickable" @click="toggleNotes()">Notes provided</span>
        </span>

        <span class="income-attribute"
              v-if="income.attachments.length">
          <paperclip-icon/>
          <span class="clickable" @click="toggleAttachments()">Attachment provided</span>
        </span>
      </div>

      <div class="income-notes" v-if="notesVisible">
        <b>Notes</b><br/>
        <!--todo linebreaks-->
        <span>{{income.notes}}</span>
      </div>

      <div class="income-attachments" v-if="attachmentsVisible">
        <b>Attachments</b><br/>
        <span v-for="attachment in attachments"
              :key="attachment.id">
          <document-link :document="attachment"/><br/>
        </span>
      </div>
    </div>

    <div class="income-amount">
      <div class="amount-value">
        <money-output :currency="totalAmount.currency"
                      :amount="totalAmount.value"/>
      </div>
      <div class="income-status">
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
  import CalendarImportIcon from 'vue-material-design-icons/CalendarImport'
  import FormatListBulletedTypeIcon from 'vue-material-design-icons/FormatListBulletedType'
  import CashIcon from 'vue-material-design-icons/Cash'
  import PercentIcon from 'vue-material-design-icons/Percent'
  import MessageBulletedIcon from 'vue-material-design-icons/MessageBulleted'
  import PaperclipIcon from 'vue-material-design-icons/Paperclip'
  import PencilIcon from 'vue-material-design-icons/Pencil'

  export default {
    name: 'IncomeOverviewPanel',

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
      income: Object
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
        if (this.income.status === 'FINALIZED') {
          return ''
        } else if (this.income.status === 'PENDING_CONVERSION') {
          return `Conversion to ${this.defaultCurrency} pending`
        } else {
          return `Waiting for actual rate`
        }
      },

      totalAmount: function () {
        if (this.income.status === 'FINALIZED') {
          return {
            value: this.income.reportedAmountInDefaultCurrency,
            currency: this.defaultCurrency
          }
        } else if (this.income.status === 'PENDING_CONVERSION') {
          return {
            value: this.income.originalAmount,
            currency: this.income.currency
          }
        } else {
          return {
            value: this.income.amountInDefaultCurrency,
            currency: this.defaultCurrency
          }
        }
      }
    },

    methods: {
      amountInDefaultCurrency: function () {
        return this.income.currency === this.defaultCurrency
            ? this.income.originalAmount : this.income.amountInDefaultCurrency
      },

      isConverted: function () {
        return this.income.currency !== this.defaultCurrency
            && this.income.amountInDefaultCurrency
      },

      getDateReceived: function () {
        return this.mediumDateFormatter(new Date(this.income.dateReceived))
      },

      toggleNotes: function () {
        this.notesVisible = !this.notesVisible
      },

      toggleAttachments: async function () {
        this.attachmentsVisible = !this.attachmentsVisible

        if (this.attachments.length === 0 && this.income.attachments && this.income.attachments.length) {
          let attachments = await api.pageRequest(`/user/workspaces/${this.workspaceId}/documents`)
              .eager()
              .eqFilter("id", this.income.attachments)
              .getPageData()
          this.attachments = this.attachments.concat(attachments)
        }
      },

      navigateToIncomeEdit: function () {
        this.$router.push({name: 'edit-income', params: {id: this.income.id}})
      }
    }
  }
</script>

<style lang="scss">
  .income {
    display: flex;
    justify-content: space-between;

    .income-info {
      padding: 20px;
      border: 1px solid #ebeef5;
      background-color: #fff;
      border-radius: 4px 2px 2px 4px;
      overflow: hidden;
      flex-grow: 1;

      .income-title {
        display: flex;
        justify-content: space-between;


        b {
          font-size: 130%;
        }

        .edit-income {
          color: #409EFF;

          .material-design-icon {
            margin-right: 3px;
            font-size: 90%;
          }
        }
      }

      .income-attributes {

        .income-attribute {
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

      .income-notes {
        margin-top: 20px;

        b {
          font-size: 105%;
        }

        span {
          font-style: italic;
        }
      }

      .income-attachments {
        margin-top: 20px;

        b {
          font-size: 105%;
        }
      }
    }

    .income-amount {
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
