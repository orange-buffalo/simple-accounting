<template>
  <div>
    <div class="sa-page-header">
      <h1>{{ $t.reporting.header() }}</h1>
    </div>

    <!-- todo #64: navigation between steps-->

    <div class="reporting-panel">
      <ElSteps
        :active="activeWizardStep"
        align-center
        finish-status="success"
      >
        <ElStep
          :title="$t.reporting.wizard.steps.selectReport.title()"
          :description="reportSelectionStepDescription"
        />
        <ElStep
          :title="$t.reporting.wizard.steps.selectDates.title()"
          :description="datesSelectionStepDescription"
        />
        <ElStep
          :title="$t.reporting.wizard.steps.viewReport.title()"
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
          :data-title="$t.reporting.wizard.buttons.select()"
          @click="selectTaxReport"
        >
          <SaIcon icon="tax" />
          <div>
            <h4>{{ $t.reporting.wizard.reports.generalTax.title() }}</h4>
            <span>{{ $t.reporting.wizard.reports.generalTax.description() }}</span>
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
          :range-separator="$t.reporting.wizard.dateRange.separator()"
          :start-placeholder="$t.reporting.wizard.dateRange.startPlaceholder()"
          :end-placeholder="$t.reporting.wizard.dateRange.endPlaceholder()"
        />
        <br>
        <br>

        <!-- todo #64: navigation -->
        <ElButton
          :disabled="selectedDateRange.length !== 2"
          @click="navigateToViewReportStep"
        >
          {{ $t.reporting.wizard.buttons.next() }}
        </ElButton>
      </div>

      <div
        v-if="viewReportActive"
        class="reporting-panel--content"
      >
        <GeneralTaxReport
          :date-range="selectedDateRange"
          @report-loaded="reportGenerationInProgress = false"
        />
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed, ref } from 'vue';
import SaIcon from '@/components/SaIcon.vue';
import GeneralTaxReport from '@/pages/reporting/GeneralTaxReport.vue';
import { apiDateString } from '@/services/api';
import { $t } from '@/services/i18n';

const SELECT_REPORT_STEP = 0;
const SELECT_DATES_STEP = 1;
const VIEW_REPORT_STEP = 2;

const TAX_REPORT = 'taxReport';

// todo #64: cleanup
const activeWizardStep = ref(SELECT_REPORT_STEP);
const selectedDateRange = ref<Array<Date>>([]);
const selectedReport = ref<string | undefined>();
const reportGenerationInProgress = ref(false);

const reportSelectionActive = computed(() => activeWizardStep.value === SELECT_REPORT_STEP);

const datesSelectionActive = computed(() => activeWizardStep.value === SELECT_DATES_STEP);

const viewReportActive = computed(() => activeWizardStep.value === VIEW_REPORT_STEP);

const reportSelectionStepDescription = computed(() => {
  if (reportSelectionActive.value) {
    return $t.value.reporting.wizard.steps.selectReport.description.select();
  }
  if (selectedReport.value === TAX_REPORT) {
    return $t.value.reporting.wizard.steps.selectReport.description.selected();
  }
  return $t.value.reporting.wizard.steps.selectReport.description.unknown();
});

const datesSelectionStepDescription = computed(() => {
  if (datesSelectionActive.value) {
    return $t.value.reporting.wizard.steps.selectDates.description.select();
  }
  if (viewReportActive.value) {
    return $t.value.reporting.wizard.steps.selectDates.description.selected(
      apiDateString(selectedDateRange.value[0]),
      apiDateString(selectedDateRange.value[1]),
    );
  }
  return null;
});

const viewReportStepStatus = computed(() => {
  if (activeWizardStep.value === VIEW_REPORT_STEP && reportGenerationInProgress.value) {
    return 'process';
  }
  if (activeWizardStep.value === VIEW_REPORT_STEP) {
    return 'success';
  }
  return null;
});

const viewReportStepDescription = computed(() => {
  if (activeWizardStep.value === VIEW_REPORT_STEP && reportGenerationInProgress.value) {
    return $t.value.reporting.wizard.steps.viewReport.description.loading();
  }
  if (activeWizardStep.value === VIEW_REPORT_STEP) {
    return $t.value.reporting.wizard.steps.viewReport.description.ready();
  }
  return null;
});

const navigateToSelectDatesStep = () => {
  activeWizardStep.value = SELECT_DATES_STEP;
};

const selectTaxReport = () => {
  selectedReport.value = TAX_REPORT;
  navigateToSelectDatesStep();
};

const navigateToViewReportStep = () => {
  activeWizardStep.value = VIEW_REPORT_STEP;
  reportGenerationInProgress.value = true;
};
</script>

<style lang="scss">
  @use "@/styles/vars.scss" as *;

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
        min-width: 45px;
        min-height: 45px;
      }

      &:hover {
        &:before {
          left: 0;
        }
      }
    }
  }
</style>
