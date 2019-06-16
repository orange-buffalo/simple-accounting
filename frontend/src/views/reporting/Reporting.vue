<template>
  <div>
    <div class="sa-page-header">
      <h1>Reporting</h1>
    </div>

    <!-- todo #64: navigation between steps-->

    <div class="reporting-panel">
      <el-steps :active="activeWizardStep" align-center finish-status="success">
        <el-step title="Select a report"
                 :description="reportSelectionStepDescription"/>
        <el-step title="Select reporting dates"
                 :description="datesSelectionStepDescription"/>
        <el-step title="View the report"
                 :description="viewReportStepDescription"
                 :status="viewReportStepStatus"/>
      </el-steps>

      <div v-if="reportSelectionActive" class="reporting-panel--content">
        <div class="reporting-panel--report-selector"
             data-title="Select"
             @click="selectTaxReport">
          <svgicon name="tax"></svgicon>
          <div>
            <h4>Tax Report</h4>
            <span>Collected and paid taxes</span>
          </div>
        </div>
      </div>

      <div v-if="datesSelectionActive" class="reporting-panel--content text-center">
        <el-date-picker
            v-model="selectedDateRange"
            type="daterange"
            align="right"
            unlink-panels
            range-separator="To"
            start-placeholder="Start date"
            end-placeholder="End date">
        </el-date-picker>
        <br/>
        <br/>

        <!-- todo #64: navigation -->
        <el-button @click="navigateToViewReportStep"
                   :disabled="selectedDateRange.length !== 2">Next
        </el-button>
      </div>

      <div v-if="viewReportActive" class="reporting-panel--content">
        <the-tax-report :date-range="selectedDateRange"
                        @report-loaded="reportGenerationInProgress = false"/>
      </div>
    </div>
  </div>
</template>

<script>
  import {withWorkspaces} from '@/components/mixins/with-workspaces'
  import TheTaxReport from './TheTaxReport'
  import {api} from '@/services/api'

  const SELECT_REPORT_STEP = 0
  const SELECT_DATES_STEP = 1
  const VIEW_REPORT_STEP = 2

  const TAX_REPORT = "taxReport"

  //todo #64: cleanup
  export default {
    name: 'Reporting',

    mixins: [withWorkspaces],

    components: {
      TheTaxReport
    },

    data: function () {
      return {
        activeWizardStep: SELECT_REPORT_STEP,
        selectedDateRange: [],
        selectedReport: null,
        reportGenerationInProgress: false
      }
    },

    computed: {
      reportSelectionActive: function () {
        return this.activeWizardStep === SELECT_REPORT_STEP
      },

      datesSelectionActive: function () {
        return this.activeWizardStep === SELECT_DATES_STEP
      },

      viewReportActive: function () {
        return this.activeWizardStep === VIEW_REPORT_STEP
      },

      taxReportSelected: function () {
        return this.selectedReport = TAX_REPORT
      },

      reportSelectionStepDescription: function () {
        if (this.reportSelectionActive) {
          return 'Please select a report'
        } else {
          if (this.selectedReport === TAX_REPORT) {
            return 'Tax Report'
          } else {
            return 'Unknown Report o_O'
          }
        }
      },

      datesSelectionStepDescription: function () {
        if (this.datesSelectionActive) {
          return 'Please select reporting date range'
        } else if (this.viewReportActive) {
          //todo #6: localize
          return `${api.dateToString(this.selectedDateRange[0])} to ${api.dateToString(this.selectedDateRange[1])}`
        }
      },

      viewReportStepStatus: function () {
        if (this.activeWizardStep === VIEW_REPORT_STEP && this.reportGenerationInProgress) {
          return "process"
        } else if (this.activeWizardStep === VIEW_REPORT_STEP) {
          return "success"
        }
      },

      viewReportStepDescription: function () {
        if (this.activeWizardStep === VIEW_REPORT_STEP && this.reportGenerationInProgress) {
          return "Loading.."
        } else if (this.activeWizardStep === VIEW_REPORT_STEP) {
          return "Ready"
        }
      }
    },

    methods: {
      selectTaxReport: function () {
        this.selectedReport = TAX_REPORT
        this.navigateToSelectDatesStep()
      },

      navigateToSelectDatesStep: function () {
        this.activeWizardStep = SELECT_DATES_STEP
      },

      navigateToSelectReportStep: function () {
        this.activeWizardStep = SELECT_REPORT_STEP
      },

      navigateToViewReportStep: function () {
        this.activeWizardStep = VIEW_REPORT_STEP
        this.reportGenerationInProgress = true
      }
    }
  }
</script>

<style lang="scss">
  @import "@/styles/main.scss";

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

      .svg-icon {
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