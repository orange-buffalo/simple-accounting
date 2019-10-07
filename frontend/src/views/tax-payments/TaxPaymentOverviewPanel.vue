<template>
  <div class="tax-payment-panel">
    <div class="tax-payment-info">
      <div class="sa-item-title-panel">
        <h3>{{taxPayment.title}}</h3>
        <span class="sa-item-edit-link"
              v-if="currentWorkspace.editable">
          <svgicon name="pencil-solid"/>
          <el-button type="text"
                     @click="navigateToTaxPaymentEdit">Edit</el-button>
        </span>
      </div>

      <div class="sa-item-attributes">

        <span class="sa-item-attribute">
          <svgicon name="calendar"/>{{reportingDate}}
          <template v-if="taxPayment.reportingDate !== taxPayment.datePaid">
            <span class="sa-secondary-text">{{datePaid}}</span>
          </template>
        </span>

        <span class="sa-item-attribute"
              v-if="taxPayment.notes">
          <svgicon name="notes"/>
          <span class="sa-clickable" @click="toggleNotes()">Notes provided</span>
        </span>

        <span class="sa-item-attribute"
              v-if="taxPayment.attachments.length">
          <svgicon name="attachment"/>
          <span class="sa-clickable" @click="toggleAttachments()">Attachment provided</span>
        </span>
      </div>

      <div class="sa-item-section" v-if="notesVisible">
        <h4>Notes</h4>
        <!--todo #80: linebreaks-->
        <span class="sa-item-additional-info">{{taxPayment.notes}}</span>
      </div>

      <div class="sa-item-section" v-if="attachmentsVisible">
        <h4>Attachments</h4>
        <span v-for="attachment in attachments"
              :key="attachment.id">
          <document-link :document="attachment"/><br/>
        </span>
      </div>
    </div>

    <div class="tax-payment-amount">
      <div class="amount-value">
        <money-output :currency="defaultCurrency"
                      :amount="taxPayment.amount"/>
      </div>
    </div>
  </div>
</template>

<script>
  import MoneyOutput from '@/components/MoneyOutput'
  import DocumentLink from '@/components/DocumentLink'
  import {withMediumDateFormatter} from '@/components/mixins/with-medium-date-formatter'
  import '@/components/icons/attachment'
  import '@/components/icons/calendar'
  import '@/components/icons/notes'
  import '@/components/icons/pencil-solid'
  import {loadDocuments} from '@/services/app-services'
  import {withWorkspaces} from '@/components/mixins/with-workspaces'

  export default {
    name: 'TaxPaymentOverviewPanel',

    mixins: [withMediumDateFormatter, withWorkspaces],

    components: {
      MoneyOutput,
      DocumentLink
    },

    props: {
      taxPayment: Object
    },

    data: function () {
      return {
        notesVisible: false,
        attachmentsVisible: false,
        attachments: []
      }
    },

    computed: {
      datePaid: function () {
        return this.mediumDateFormatter(new Date(this.taxPayment.datePaid))
      },

      reportingDate: function () {
        return this.mediumDateFormatter(new Date(this.taxPayment.reportingDate))
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
            this.taxPayment.attachments,
            this.currentWorkspace.id)
      },

      navigateToTaxPaymentEdit: function () {
        this.$router.push({name: 'edit-tax-payment', params: {id: this.taxPayment.id}})
      }
    }
  }
</script>

<style lang="scss">
  @import "@/styles/main.scss";

  .tax-payment-panel {
    display: flex;
    justify-content: space-between;
  }

  .tax-payment-info {
    @extend .sa-item-info-panel;
    border-radius: 2px 1px 1px 2px;
    flex-grow: 1;
  }

  .tax-payment-amount {
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
