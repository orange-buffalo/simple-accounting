<template>
  <div>
    <div class="sa-page-header">
      <h1>Reporting</h1>
    </div>

    <!-- todo #64: navigation between steps-->

    <div class="reporting-panel">
      <ElSteps
        :active="activeWizardStep"
        align-center
        finish-status="success"
      >
        <ElStep
          title="Select a report"
          :description="reportSelectionStepDescription"
        />
        <ElStep
          title="Select reporting dates"
          :description="datesSelectionStepDescription"
        />
        <ElStep
          title="View the report"
          :description="viewReportStepDescription"
          :status="viewReportStepStatus"
        />
      </ElSteps>

      <div
        v-if="reportSelectionActive"
        class="reporting-panel--content"
      >
        <div
          class="reporting-panel--report-selector"
          data-title="Select"
          @click="selectTaxReport"
        >
          <SaIcon icon="tax" />
          <div>
            <h4>General Tax Report</h4>
            <span>Collected and paid general taxes</span>
          </div>
        </div>
      </div>

      <div
        v-if="datesSelectionActive"
        class="reporting-panel--content text-center"
      >
        <ElDatePicker
          v-model="selectedDateRange"
          type="daterange"
          align="right"
          unlink-panels
          range-separator="To"
          start-placeholder="Start date"
          end-placeholder="End date"
        />
        <br>
        <br>

        <!-- todo #64: navigation -->
        <ElButton
          :disabled="selectedDateRange.length !== 2"
          @click="navigateToViewReportStep"
        >
          Next
        </ElButton>
      </div>

      <div
        v-if="viewReportActive"
        class="reporting-panel--content"
      >
        <TheGeneralTaxReport
          :date-range="selectedDateRange"
          @report-loaded="reportGenerationInProgress = false"
        />
      </div>
    </div>
  </div>
</template>

<script>
  import { api } from '@/services/api-legacy';
  import TheGeneralTaxReport from '@/views/reporting/TheGeneralTaxReport';
  import SaIcon from '@/components/SaIcon';

  const SELECT_REPORT_STEP = 0;
  const SELECT_DATES_STEP = 1;
  const VIEW_REPORT_STEP = 2;

  const TAX_REPORT = 'taxReport';

  // todo #64: cleanup
  export default {
    name: 'Reporting',

    components: {
      SaIcon,
      TheGeneralTaxReport,
    },

    data() {
      return {
        activeWizardStep: SELECT_REPORT_STEP,
        selectedDateRange: [],
        selectedReport: null,
        reportGenerationInProgress: false,
      };
    },

    computed: {
      reportSelectionActive() {
        return this.activeWizardStep === SELECT_REPORT_STEP;
      },

      datesSelectionActive() {
        return this.activeWizardStep === SELECT_DATES_STEP;
      },

      viewReportActive() {
        return this.activeWizardStep === VIEW_REPORT_STEP;
      },

      taxReportSelected() {
        return this.selectedReport === TAX_REPORT;
      },

      reportSelectionStepDescription() {
        if (this.reportSelectionActive) {
          return 'Please select a report';
        }
        if (this.selectedReport === TAX_REPORT) {
          return 'Tax Report';
        }
        return 'Unknown Report o_O';
      },

      datesSelectionStepDescription() {
        if (this.datesSelectionActive) {
          return 'Please select reporting date range';
        }
        if (this.viewReportActive) {
          // todo #64: localize
          return `${api.dateToString(this.selectedDateRange[0])} to ${api.dateToString(this.selectedDateRange[1])}`;
        }
        return null;
      },

      viewReportStepStatus() {
        if (this.activeWizardStep === VIEW_REPORT_STEP && this.reportGenerationInProgress) {
          return 'process';
        }
        if (this.activeWizardStep === VIEW_REPORT_STEP) {
          return 'success';
        }
        return null;
      },

      viewReportStepDescription() {
        if (this.activeWizardStep === VIEW_REPORT_STEP && this.reportGenerationInProgress) {
          return 'Loading..';
        }
        if (this.activeWizardStep === VIEW_REPORT_STEP) {
          return 'Ready';
        }
        return null;
      },
    },

    methods: {
      selectTaxReport() {
        this.selectedReport = TAX_REPORT;
        this.navigateToSelectDatesStep();
      },

      navigateToSelectDatesStep() {
        this.activeWizardStep = SELECT_DATES_STEP;
      },

      navigateToSelectReportStep() {
        this.activeWizardStep = SELECT_REPORT_STEP;
      },

      navigateToViewReportStep() {
        this.activeWizardStep = VIEW_REPORT_STEP;
        this.reportGenerationInProgress = true;
      },
    },
  };
</script>

<style lang="scss">
  @import "~@/styles/main.scss";

  .reporting-panel {
    padding: 20px;
    border: 1px solid $secondary-grey;
    background-color: $white;
    border-radius: 2px;
    overflow: hidden;

    &--content {
      margin-top: 20px;

      h4 {
        margin: 0;
      }
    }

    &--report-selector {
      border: 1px solid $secondary-grey;
      padding: 10px;
      display: flex;
      align-items: center;
      max-width: 200px;
      position: relative;
      overflow: hidden;

      &:before {
        content: attr(data-title);
        position: absolute;
        width: 100%;
        height: 100%;
        left: 100%;
        top: 0;
        background-color: $white;
        transition: all 0.25s;
        display: flex;
        justify-content: center;
        align-items: center;
        font-weight: bold;
        cursor: pointer;
      }

      .sa-icon {
        margin: 0 10px 0 0;
        width: 45px;
        height: 45px;
      }

      &:hover {
        &:before {
          left: 0;
        }
      }
    }
  }
</style>
