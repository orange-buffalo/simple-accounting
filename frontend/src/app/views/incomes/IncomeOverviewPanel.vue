<template>
  <div class="income-panel">
    <div class="income-info">
      <div class="item-title-panel">
        <h3>{{income.title}}</h3>
        <span class="item-edit-link">
          <pencil-icon/>
          <el-button type="text"
                     @click="navigateToIncomeEdit">Edit</el-button>
        </span>
      </div>

      <div class="item-attributes">

        <span class="item-attribute">
          <format-list-bulleted-type-icon/>{{ categoryById(income.category).name }}
        </span>

        <span class="item-attribute">
          <calendar-import-icon/>{{getDateReceived()}}
        </span>

        <span class="item-attribute">
          <cash-icon/>
          <money-output :currency="defaultCurrency"
                        :amount="amountInDefaultCurrency()"/>

          <template v-if="isConverted()">
            <money-output :currency="income.currency"
                          :amount="income.originalAmount"
                          class="secondary-text"/>
          </template>
        </span>

        <span class="item-attribute"
              v-if="income.notes">
          <message-bulleted-icon/>
          <span class="clickable" @click="toggleNotes()">Notes provided</span>
        </span>

        <span class="item-attribute"
              v-if="income.attachments.length">
          <paperclip-icon/>
          <span class="clickable" @click="toggleAttachments()">Attachment provided</span>
        </span>
      </div>

      <div class="item-section" v-if="notesVisible">
        <h4>Notes</h4>
        <!--todo linebreaks-->
        <span class="item-additional-info">{{income.notes}}</span>
      </div>

      <div class="item-section" v-if="attachmentsVisible">
        <h4>Attachments</h4>
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
  @import "@/app/main.scss";

  .income-panel {
    display: flex;
    justify-content: space-between;
  }

  .income-info {
    @extend .item-info-panel;
    border-radius: 4px 2px 2px 4px;
    flex-grow: 1;
  }

  .income-amount {
    @extend .item-info-panel;
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
