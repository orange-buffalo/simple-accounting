<template>
  <div class="tax-payment-panel">
    <div class="tax-payment-info">
      <div class="sa-item-title-panel">
        <h3>{{taxPayment.title}}</h3>
        <span class="sa-item-edit-link">
          <svgicon name="pencil"/>
          <el-button type="text"
                     @click="navigateToTaxPaymentEdit">Edit</el-button>
        </span>
      </div>

      <div class="sa-item-attributes">

        <span class="sa-item-attribute">
          <svgicon name="calendar"/>{{getDatePaid()}}
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
        <!--todo linebreaks-->
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
  import {mapState} from 'vuex'
  import MoneyOutput from '@/app/components/MoneyOutput'
  import DocumentLink from '@/app/components/DocumentLink'
  import withMediumDateFormatter from '@/app/components/mixins/with-medium-date-formatter'
  import api from '@/services/api'
  import '@/components/icons/attachment'
  import '@/components/icons/calendar'
  import '@/components/icons/notes'
  import '@/components/icons/pencil'

  export default {
    name: 'TaxPaymentOverviewPanel',

    mixins: [withMediumDateFormatter],

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
      ...mapState({
        workspaceId: state => state.workspaces.currentWorkspace.id,
        defaultCurrency: state => state.workspaces.currentWorkspace.defaultCurrency
      })
    },

    methods: {
         getDatePaid: function () {
        return this.mediumDateFormatter(new Date(this.taxPayment.datePaid))
      },

      toggleNotes: function () {
        this.notesVisible = !this.notesVisible
      },

      toggleAttachments: async function () {
        this.attachmentsVisible = !this.attachmentsVisible

        if (this.attachments.length === 0 && this.taxPayment.attachments && this.taxPayment.attachments.length) {
          let attachments = await api.pageRequest(`/user/workspaces/${this.workspaceId}/documents`)
              .eager()
              .eqFilter("id", this.taxPayment.attachments)
              .getPageData()
          this.attachments = this.attachments.concat(attachments)
        }
      },

      navigateToTaxPaymentEdit: function () {
        this.$router.push({name: 'edit-tax-payment', params: {id: this.taxPayment.id}})
      }
    }
  }
</script>

<style lang="scss">
  @import "@/app/main.scss";

  .tax-payment-panel {
    display: flex;
    justify-content: space-between;
  }

  .tax-payment-info {
    @extend .sa-item-info-panel;
    border-radius: 4px 2px 2px 4px;
    flex-grow: 1;
  }

  .tax-payment-amount {
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
