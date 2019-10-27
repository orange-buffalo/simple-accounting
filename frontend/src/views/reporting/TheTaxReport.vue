<template>
  <div>
    <h2>Collected</h2>
    <the-tax-report-table :data="collectedTaxes" />
    <div
      v-if="report"
      class="text-right"
    >
      <br>
      <b>Total:
        <money-output
          :currency="defaultCurrency"
          :amount="totalCollectedAmount"
        />
      </b>
    </div>
    <br>

    <h2>Paid</h2>
    <the-tax-report-table :data="paidTaxes" />
    <div
      v-if="report"
      class="text-right"
    >
      <br>
      <b>Total:
        <money-output
          :currency="defaultCurrency"
          :amount="totalPaidAmount"
        />
      </b>
    </div>
  </div>
</template>

<script>
import { assign, isNil } from 'lodash';

import { withWorkspaces } from '@/components/mixins/with-workspaces';
import { withTaxes } from '@/components/mixins/with-taxes';
import MoneyOutput from '@/components/MoneyOutput';
import TheTaxReportTable from '@/views/reporting/TheTaxReportTable';
import { reportGenerator } from '@/views/reporting/report-generator';

export default {
  name: 'TheTaxReport',

  components: {
    TheTaxReportTable,
    MoneyOutput,
  },

  mixins: [withWorkspaces, withTaxes, reportGenerator],

  computed: {
    collectedTaxes() {
      return this.$transformTaxes(true);
    },

    paidTaxes() {
      return this.$transformTaxes(false);
    },

    totalCollectedAmount() {
      return this.$getTotalAmount(this.report.finalizedCollectedTaxes);
    },

    totalPaidAmount() {
      return this.$getTotalAmount(this.report.finalizedPaidTaxes);
    },
  },

  methods: {
    reload(api, fromDate, toDate) {
      return api.get(`/workspaces/${this.currentWorkspace.id}/reporting/taxes`
            + `?fromDate=${fromDate}&toDate=${toDate}`);
    },

    $transformTaxes(collected) {
      if (isNil(this.report)) {
        return [];
      }
      const finalizedTaxes = collected ? this.report.finalizedCollectedTaxes : this.report.finalizedPaidTaxes;
      const pendingTaxes = collected ? this.report.pendingCollectedTaxes : this.report.pendingPaidTaxes;

      let taxes = finalizedTaxes.map(tax => assign({}, tax, {
        finalized: true,
        tax: this.taxById(tax.tax),
      }));

      taxes = taxes.concat(pendingTaxes.map(tax => assign({}, tax, {
        finalized: false,
        tax: this.taxById(tax.tax),
      })));

      return taxes;
    },

    $getTotalAmount(taxes) {
      return taxes.map(tax => tax.taxAmount).reduce((it, sum) => sum + it, 0);
    },
  },
};
</script>
