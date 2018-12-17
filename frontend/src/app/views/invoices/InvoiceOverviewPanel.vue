<template>
  <div class="invoice-panel">
    <div class="invoice-info">
      <div class="sa-item-title-panel">
        <h3>{{invoice.title}}</h3>
        <span class="sa-item-edit-link">
          <svgicon name="pencil"/>
          <el-button type="text"
                     @click="navigateToInvoiceEdit">Edit</el-button>
        </span>
      </div>

      <div class="sa-item-attributes">

        <el-tooltip content="Customer" v-if="customer">
          <span class="sa-item-attribute">
            <svgicon name="category"/>{{ customer.name }}
          </span>
        </el-tooltip>

        <el-tooltip content="Date Issued" v-if="!invoice.dateCancelled">
          <span class="sa-item-attribute">
            <svgicon name="calendar"/>{{dateIssued}}
          </span>
        </el-tooltip>

        <el-tooltip content="Due Date" v-if="!invoice.dateCancelled">
          <span class="sa-item-attribute">
            <svgicon name="calendar"/>{{dueDate}}
          </span>
        </el-tooltip>

        <el-tooltip content="Date Sent" v-if="!invoice.dateCancelled && invoice.dateSent">
          <span class="sa-item-attribute">
            <svgicon name="calendar"/>{{dateSent}}
          </span>
        </el-tooltip>

        <el-tooltip content="Date Paid" v-if="!invoice.dateCancelled && invoice.datePaid">
          <span class="sa-item-attribute">
            <svgicon name="calendar"/>{{datePaid}}
          </span>
        </el-tooltip>

        <span class="sa-item-attribute"
              v-if="invoice.notes">
          <svgicon name="notes"/>
          <span class="sa-clickable" @click="toggleNotes()">Notes provided</span>
        </span>

        <span class="sa-item-attribute"
              v-if="invoice.attachments.length">
          <svgicon name="attachment"/>
          <span class="sa-clickable" @click="toggleAttachments()">Attachment provided</span>
        </span>
      </div>

      <div class="sa-item-section" v-if="notesVisible">
        <h4>Notes</h4>
        <!--todo linebreaks-->
        <span class="sa-item-additional-info">{{invoice.notes}}</span>
      </div>

      <div class="sa-item-section" v-if="attachmentsVisible">
        <h4>Attachments</h4>
        <span v-for="attachment in attachments"
              :key="attachment.id">
          <document-link :document="attachment"/><br/>
        </span>
      </div>
    </div>

    <div class="invoice-amount">
      <div class="amount-value">
        <money-output :currency="invoice.currency"
                      :amount="invoice.amount"/>
      </div>
      <div class="invoice-status">
        {{status}}
      </div>
      <div v-if="isDraft">
        <el-button @click="markSent">Sent today</el-button>
      </div>
      <div v-if="isSent || isOverdue">
        <span>Due on {{dueDate}}</span><br/>
        <el-button @click="markPaid">Paid today</el-button>
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
  import '@/components/icons/category'
  import '@/components/icons/pencil'

  export default {
    name: 'InvoiceOverviewPanel',

    mixins: [withMediumDateFormatter],

    components: {
      MoneyOutput,
      DocumentLink
    },

    props: {
      invoice: Object
    },

    data: function () {
      return {
        notesVisible: false,
        attachmentsVisible: false,
        attachments: [],
        customer: null
      }
    },

    computed: {
      ...mapState({
        workspaceId: state => state.workspaces.currentWorkspace.id
      }),

      isDraft: function () {
        return this.invoice.status === 'DRAFT'
      },

      isPaid: function () {
        return this.invoice.status === 'PAID'
      },

      isCancelled: function () {
        return this.invoice.status === 'CANCELLED'
      },

      isSent: function () {
        return this.invoice.status === 'SENT'
      },

      isOverdue: function () {
        return this.invoice.status === 'OVERDUE'
      },

      status: function () {
        if (this.isPaid) {
          return ''
        } else if (this.isDraft) {
          return `Draft`
        } else if (this.isCancelled) {
          return `Cancelled`
        } else if (this.isSent) {
          return `Sent`
        } else if (this.isOverdue) {
          return `Overdue`
        } else {
          return `??`
        }
      },

      dateIssued: function () {
        return this.mediumDateFormatter(new Date(this.invoice.dateIssued))
      },

      dueDate: function () {
        return this.mediumDateFormatter(new Date(this.invoice.dueDate))
      },

      dateSent: function () {
        return this.mediumDateFormatter(new Date(this.invoice.dateSent))
      },

      datePaid: function () {
        return this.mediumDateFormatter(new Date(this.invoice.datePaid))
      },

      dateCancelled: function () {
        return this.mediumDateFormatter(new Date(this.invoice.dateCancelled))
      }
    },

    created: async function () {
      this.customer = (await api.get(`/user/workspaces/${this.workspaceId}/customers/${this.invoice.customer}`)).data
    },

    methods: {
      toggleNotes: function () {
        this.notesVisible = !this.notesVisible
      },

      toggleAttachments: async function () {
        this.attachmentsVisible = !this.attachmentsVisible

        if (this.attachments.length === 0 && this.invoice.attachments && this.invoice.attachments.length) {
          let attachments = await api.pageRequest(`/user/workspaces/${this.workspaceId}/documents`)
              .eager()
              .eqFilter("id", this.invoice.attachments)
              .getPageData()
          this.attachments = this.attachments.concat(attachments)
        }
      },

      navigateToInvoiceEdit: function () {
        this.$router.push({name: 'edit-invoice', params: {id: this.invoice.id}})
      },

      markSent: async function () {
        this.invoice.dateSent = api.dateToString(new Date())
        await api.put(`/user/workspaces/${this.workspaceId}/invoices/${this.invoice.id}`, this.invoice)
        this.$emit('invoice-update')
      },

      markPaid: async function () {
        this.invoice.datePaid = api.dateToString(new Date())
        await api.put(`/user/workspaces/${this.workspaceId}/invoices/${this.invoice.id}`, this.invoice)
        this.$emit('invoice-update')
      }
    }
  }
</script>

<style lang="scss">
  @import "@/app/main.scss";

  .invoice-panel {
    display: flex;
    justify-content: space-between;
  }

  .invoice-info {
    @extend .sa-item-info-panel;
    border-radius: 4px 2px 2px 4px;
    flex-grow: 1;
  }

  .invoice-amount {
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
