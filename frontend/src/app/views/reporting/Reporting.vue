<template>
  <div>
    <div class="sa-page-header">
      <h1>Reporting</h1>
    </div>

    <!--    todo navigation between steps-->

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

      <div v-if="reportSelectionActive">
        <!--        todo style for report selection -->
        <el-button @click="selectTaxReport">Tax Report</el-button>
      </div>

      <div v-if="datesSelectionActive">
        <el-date-picker
            v-model="selectedDateRange"
            type="daterange"
            align="right"
            unlink-panels
            range-separator="To"
            start-placeholder="Start date"
            end-placeholder="End date">
        </el-date-picker>

        <!--        todo navigation -->
        <el-button @click="navigateToViewReportStep"
                   :disabled="selectedDateRange.length !== 2">Next
        </el-button>
      </div>

      <div v-if="viewReportActive">
        <the-tax-report :date-range="selectedDateRange"
                        @report-loaded="reportGenerationInProgress = false"/>
      </div>
    </div>
  </div>
</template>

<script>
  import {withWorkspaces} from '@/app/components/mixins/with-workspaces'
  import TheTaxReport from './TheTaxReport'
  import {api} from '@/services/api'

  const SELECT_REPORT_STEP = 0
  const SELECT_DATES_STEP = 1
  const VIEW_REPORT_STEP = 2

  const TAX_REPORT = "taxReport"

  //todo: cleanup
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
          //todo localize
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
  @import "@/app/styles/main.scss";

  .reporting-panel {
    padding: 20px;
    border: 1px solid $secondary-grey;
    background-color: $white;
    border-radius: 2px;
    overflow: hidden;
  }
</style>