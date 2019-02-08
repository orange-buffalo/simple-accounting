<template>
  <div class="income-panel">
    <div class="income-info">
      <div class="sa-item-title-panel">
        <h3>{{income.title}}</h3>
        <span class="sa-item-edit-link">
          <svgicon name="pencil"/>
          <el-button type="text"
                     @click="navigateToIncomeEdit">Edit</el-button>
        </span>
      </div>

      <div class="sa-item-attributes">

        <span class="sa-item-attribute">
          <svgicon name="category"/>{{ categoryById(income.category).name }}
        </span>

        <span class="sa-item-attribute">
          <svgicon name="calendar"/>{{dateReceived}}
        </span>

        <span class="sa-item-attribute">
          <svgicon name="banknote"/>
          <money-output :currency="defaultCurrency"
                        :amount="amountInDefaultCurrency"/>

          <template v-if="isConverted">
            <money-output :currency="income.currency"
                          :amount="income.originalAmount"
                          class="sa-secondary-text"/>
          </template>
        </span>

        <span class="sa-item-attribute"
              v-if="income.linkedInvoice">
          <svgicon name="invoice"/>
          <span>{{income.linkedInvoice.title}}</span>
        </span>

        <span class="sa-item-attribute"
              v-if="income.notes">
          <svgicon name="notes"/>
          <span class="sa-clickable" @click="toggleNotes()">Notes provided</span>
        </span>

        <span class="sa-item-attribute"
              v-if="income.attachments.length">
          <svgicon name="attachment"/>
          <span class="sa-clickable" @click="toggleAttachments()">Attachment provided</span>
        </span>
      </div>

      <div class="sa-item-section" v-if="notesVisible">
        <h4>Notes</h4>
        <!--todo linebreaks-->
        <span class="sa-item-additional-info">{{income.notes}}</span>
      </div>

      <div class="sa-item-section" v-if="attachmentsVisible">
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
  import MoneyOutput from '@/app/components/MoneyOutput'
  import DocumentLink from '@/app/components/DocumentLink'
  import {withMediumDateFormatter} from '@/app/components/mixins/with-medium-date-formatter'
  import '@/components/icons/attachment'
  import '@/components/icons/banknote'
  import '@/components/icons/calendar'
  import '@/components/icons/notes'
  import '@/components/icons/category'
  import '@/components/icons/pencil'
  import '@/components/icons/percent'
  import '@/components/icons/invoice'
  import {withCategories} from '@/app/components/mixins/with-categories'
  import {withWorkspaces} from '@/app/components/mixins/with-workspaces'
  import {loadDocuments} from '@/app/services/app-services'

  export default {
    name: 'IncomeOverviewPanel',

    mixins: [withMediumDateFormatter, withWorkspaces, withCategories],

    components: {
      MoneyOutput,
      DocumentLink
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
      },
      amountInDefaultCurrency: function () {
        return this.income.currency === this.defaultCurrency
            ? this.income.originalAmount : this.income.amountInDefaultCurrency
      },

      isConverted: function () {
        return this.income.currency !== this.defaultCurrency
            && this.income.amountInDefaultCurrency
      },

      dateReceived: function () {
        return this.mediumDateFormatter(new Date(this.income.dateReceived))
      }
    },

    methods: {
      toggleNotes: function () {
        this.notesVisible = !this.notesVisible
      },

      toggleAttachments: async function () {
        this.attachmentsVisible = !this.attachmentsVisible

        this.attachments = await loadDocuments(
            this.attachments,
            this.income.attachments,
            this.currentWorkspace.id)
      },

      navigateToIncomeEdit: function () {
        this.$router.push({name: 'edit-income', params: {id: this.income.id}})
      }
    }
  }
</script>

<style lang="scss">
  @import "@/app/styles/main.scss";

  .income-panel {
    display: flex;
    justify-content: space-between;
  }

  .income-info {
    @extend .sa-item-info-panel;
    border-radius: 2px 1px 1px 2px;
    flex-grow: 1;
  }

  .income-amount {
    @extend .sa-item-info-panel;
    width: 15%;
    border-radius: 1px 2px 2px 1px;
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
