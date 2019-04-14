<template>
  <div>
    <h2>Collected</h2>
    <div v-if="report">
      Total:
      <money-output :currency="defaultCurrency"
                    :amount="totalCollectedAmount"/>
    </div>
    <the-tax-report-table :data="collectedTaxes"/>

    <h2>Paid</h2>
    <div v-if="report">
      Total:
      <money-output :currency="defaultCurrency"
                    :amount="totalPaidAmount"/>
    </div>
    <the-tax-report-table :data="paidTaxes"/>

  </div>
</template>

<script>
  import {assign} from 'lodash'
  import {isNil} from 'lodash'
  import {withWorkspaces} from '@/app/components/mixins/with-workspaces'
  import {withTaxes} from '@/app/components/mixins/with-taxes'
  import MoneyOutput from '@/app/components/MoneyOutput'
  import TheTaxReportTable from '@/app/views/reporting/TheTaxReportTable'
  import {reportGenerator} from '@/app/views/reporting/report-generator'

  export default {
    name: 'TheTaxReport',

    mixins: [withWorkspaces, withTaxes, reportGenerator],

    components: {
      TheTaxReportTable,
      MoneyOutput
    },

    computed: {
      collectedTaxes: function () {
        return this.$transformTaxes(true)
      },

      paidTaxes: function () {
        return this.$transformTaxes(false)
      },

      totalCollectedAmount: function () {
        return this.$getTotalAmount(this.report.finalizedCollectedTaxes)
      },

      totalPaidAmount: function () {
        return this.$getTotalAmount(this.report.finalizedPaidTaxes)
      }
    },

    methods: {
      reload: function (api, fromDate, toDate) {
        return api.get(`/user/workspaces/${this.currentWorkspace.id}/reporting/taxes` +
            `?fromDate=${fromDate}&toDate=${toDate}`)
      },

      $transformTaxes: function (collected) {
        if (isNil(this.report)) {
          return []
        }
        let finalizedTaxes = collected ? this.report.finalizedCollectedTaxes : this.report.finalizedPaidTaxes
        let pendingTaxes = collected ? this.report.pendingCollectedTaxes : this.report.pendingPaidTaxes

        let taxes = finalizedTaxes.map(tax => assign({}, tax, {
          finalized: true,
          tax: this.taxById(tax.tax)
        }))

        taxes = taxes.concat(pendingTaxes.map(tax => assign({}, tax, {
          finalized: false,
          tax: this.taxById(tax.tax)
        })))

        return taxes
      },

      $getTotalAmount: function (taxes) {
        return taxes.map(tax => tax.taxAmount).reduce((it, sum) => sum + it, 0)
      }
    }
  }
</script>