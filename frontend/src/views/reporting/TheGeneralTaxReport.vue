<template>
  <div>
    <h2>Collected</h2>
    <TheGeneralTaxReportTable :data="collectedTaxes" />
    <div
      v-if="report"
      class="text-right"
    >
      <br>
      <b>Total:
        <MoneyOutput
          :currency="defaultCurrency"
          :amount="totalCollectedAmount"
        />
      </b>
    </div>
    <br>

    <h2>Paid</h2>
    <TheGeneralTaxReportTable :data="paidTaxes" />
    <div
      v-if="report"
      class="text-right"
    >
      <br>
      <b>Total:
        <MoneyOutput
          :currency="defaultCurrency"
          :amount="totalPaidAmount"
        />
      </b>
    </div>
  </div>
</template>

<script>
  import withWorkspaces from '@/components/mixins/with-workspaces';
  import MoneyOutput from '@/components/MoneyOutput';
  import { reportGenerator } from '@/views/reporting/report-generator';
  import TheGeneralTaxReportTable from '@/views/reporting/TheGeneralTaxReportTable';

  export default {
    name: 'TheGeneralTaxReport',

    components: {
      TheGeneralTaxReportTable,
      MoneyOutput,
    },

    mixins: [withWorkspaces, reportGenerator],

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
        return api.get(`/workspaces/${this.currentWorkspace.id}/reporting/general-taxes`
          + `?fromDate=${fromDate}&toDate=${toDate}`);
      },

      $transformTaxes(collected) {
        if (this.report == null) {
          return [];
        }
        const finalizedTaxes = collected ? this.report.finalizedCollectedTaxes : this.report.finalizedPaidTaxes;
        const pendingTaxes = collected ? this.report.pendingCollectedTaxes : this.report.pendingPaidTaxes;

        let taxes = finalizedTaxes.map(tax => ({
          ...tax,
          finalized: true,
          taxId: tax.tax,
        }));

        taxes = taxes.concat(pendingTaxes.map(tax => ({
          ...tax,
          finalized: false,
          taxId: tax.tax,
        })));

        return taxes;
      },

      $getTotalAmount(taxes) {
        return taxes.map(tax => tax.taxAmount)
          .reduce((it, sum) => sum + it, 0);
      },
    },
  };
</script>
