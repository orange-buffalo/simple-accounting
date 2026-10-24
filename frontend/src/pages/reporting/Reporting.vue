<template>
  <SaPage :header="$t.reporting.header()">

    <SaWizard v-model="activeWizardStep" :steps="wizardSteps" class="reporting-panel">
      <template #select-report="{ navigate }">
        <div class="reporting-panel--content reporting-panel--selection">
          <p class="reporting-panel--selection-intro">
            {{ $t.reporting.wizard.selectionIntro() }}
          </p>
          <div class="reporting-panel--report-selectors">
            <button
              type="button"
              class="reporting-panel--report-selector"
              @click="selectReport(GENERAL_TAX_REPORT); navigate(SELECT_DATES_STEP)"
            >
              <span class="reporting-panel--report-icon" aria-hidden="true"><SaIcon icon="tax" :size="42" /></span>
              <span class="reporting-panel--report-copy">
                <span class="reporting-panel--report-title">{{ $t.reporting.wizard.reports.generalTax.title() }}</span>
                <span class="reporting-panel--report-description">{{ $t.reporting.wizard.reports.generalTax.description() }}</span>
              </span>
              <span class="reporting-panel--report-action" aria-hidden="true">{{ $t.reporting.wizard.buttons.select() }} <span>→</span></span>
            </button>
            <button
              type="button"
              class="reporting-panel--report-selector"
              @click="selectReport(INCOME_TAX_REPORT); navigate(SELECT_DATES_STEP)"
            >
              <span class="reporting-panel--report-icon" aria-hidden="true"><SaIcon icon="reporting" :size="42" /></span>
              <span class="reporting-panel--report-copy">
                <span class="reporting-panel--report-title">{{ $t.reporting.wizard.reports.incomeTax.title() }}</span>
                <span class="reporting-panel--report-description">{{ $t.reporting.wizard.reports.incomeTax.description() }}</span>
              </span>
              <span class="reporting-panel--report-action" aria-hidden="true">{{ $t.reporting.wizard.buttons.select() }} <span>→</span></span>
            </button>
          </div>
        </div>
      </template>
      <template #select-dates>
        <div class="reporting-panel--content reporting-panel--dates">
          <div class="reporting-panel--selected-range" aria-live="polite">
            <span>{{ displayedDateRange[0] ? $t.common.date.medium(displayedDateRange[0]) : $t.reporting.wizard.dateRange.startPlaceholder() }}</span>
            <span aria-hidden="true">—</span>
            <span>{{ displayedDateRange[1] ? $t.common.date.medium(displayedDateRange[1]) : $t.reporting.wizard.dateRange.endPlaceholder() }}</span>
          </div>
          <div class="reporting-panel--calendar-container">
            <ElDatePickerPanel
              v-model="selectedDateRange"
              type="daterange"
              :default-value="selectedDateRange[0]"
              :border="false"
              :clearable="false"
              class="reporting-panel--calendar"
              @calendar-change="onCalendarChange"
              @update:model-value="onRangeSelected"
            />
          </div>
        </div>
      </template>
      <template #view-report>
        <div class="reporting-panel--content">
          <GeneralTaxReport
            v-if="selectedReport === GENERAL_TAX_REPORT"
            :date-range="selectedDateRange"
            @report-loaded="reportGenerationInProgress = false"
          />
          <IncomeTaxReport
            v-if="selectedReport === INCOME_TAX_REPORT"
            :date-range="selectedDateRange"
            @report-loaded="reportGenerationInProgress = false"
          />
        </div>
      </template>
    </SaWizard>
  </SaPage>
</template>

<script lang="ts" setup>
  import { computed, ref } from 'vue';
  import SaPage from '@/components/SaPage.vue';
  import GeneralTaxReport from '@/pages/reporting/GeneralTaxReport.vue';
  import IncomeTaxReport from '@/pages/reporting/IncomeTaxReport.vue';
  import SaIcon from '@/components/SaIcon.vue';
  import { apiDateString } from '@/services/api';
  import { $t } from '@/services/i18n';
  import { getAustralianFinancialYearDateRange } from '@/services/date-utils';
  import { ElDatePickerPanel } from 'element-plus';
  import SaWizard from '@/components/wizard/SaWizard.vue';
  import type { WizardStep } from '@/components/wizard/sa-wizard';

  const SELECT_REPORT_STEP = 'select-report';
  const SELECT_DATES_STEP = 'select-dates';
  const VIEW_REPORT_STEP = 'view-report';
  type ReportStep = typeof SELECT_REPORT_STEP | typeof SELECT_DATES_STEP | typeof VIEW_REPORT_STEP;

  const GENERAL_TAX_REPORT = 'generalTaxReport';
  const INCOME_TAX_REPORT = 'incomeTaxReport';
  type Report = typeof GENERAL_TAX_REPORT | typeof INCOME_TAX_REPORT;

  const activeWizardStep = ref<ReportStep>(SELECT_REPORT_STEP);
  const selectedDateRange = ref<Array<Date>>([]);
  const rangeSelectionInProgress = ref(false);
  const pendingDateRange = ref<Array<Date | null> | null>(null);
  const displayedDateRange = computed(() => pendingDateRange.value ?? selectedDateRange.value);
  const selectedReport = ref<Report>();
  const reportGenerationInProgress = ref(false);

  const reportSelectionActive = computed(() => activeWizardStep.value === SELECT_REPORT_STEP);

  const datesSelectionActive = computed(() => activeWizardStep.value === SELECT_DATES_STEP);

  const viewReportActive = computed(() => activeWizardStep.value === VIEW_REPORT_STEP);

  const reportSelectionStepDescription = computed(() => {
    if (reportSelectionActive.value) {
      return $t.value.reporting.wizard.steps.selectReport.description.select();
    }
    if (selectedReport.value === GENERAL_TAX_REPORT) {
      return $t.value.reporting.wizard.steps.selectReport.description.selected();
    }
    if (selectedReport.value === INCOME_TAX_REPORT) {
      return $t.value.reporting.wizard.reports.incomeTax.title();
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
        apiDateString(selectedDateRange.value[1])
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
    return undefined;
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

  const wizardSteps = computed<WizardStep<ReportStep>[]>(() => [
    {
      id: SELECT_REPORT_STEP,
      title: $t.value.reporting.wizard.steps.selectReport.title(),
      description: reportSelectionStepDescription.value,
    },
    {
      id: SELECT_DATES_STEP,
      title: $t.value.reporting.wizard.steps.selectDates.title(),
      description: datesSelectionStepDescription.value,
      navigationDisabled: !selectedReport.value,
      back: { label: $t.value.common.back(), target: SELECT_REPORT_STEP },
      next: {
        label: $t.value.reporting.wizard.buttons.next(),
        target: VIEW_REPORT_STEP,
        disabled: selectedDateRange.value.length !== 2 || rangeSelectionInProgress.value,
      },
    },
    {
      id: VIEW_REPORT_STEP,
      title: $t.value.reporting.wizard.steps.viewReport.title(),
      description: viewReportStepDescription.value,
      status: viewReportStepStatus.value,
      navigationDisabled: !selectedReport.value || selectedDateRange.value.length !== 2
        || rangeSelectionInProgress.value,
      onEnter: () => { reportGenerationInProgress.value = true; },
      back: { label: $t.value.common.back(), target: SELECT_DATES_STEP },
    },
  ]);

  const onCalendarChange = (dates: Array<Date | null>) => {
    rangeSelectionInProgress.value = dates.length !== 2 || dates.some((date) => date == null);
    pendingDateRange.value = rangeSelectionInProgress.value ? dates : null;
  };

  const onRangeSelected = () => {
    rangeSelectionInProgress.value = false;
    pendingDateRange.value = null;
  };

  const selectReport = (report: Report) => {
    selectedReport.value = report;
    rangeSelectionInProgress.value = false;
    pendingDateRange.value = null;
    selectedDateRange.value = report === INCOME_TAX_REPORT
      ? getAustralianFinancialYearDateRange()
      : [];
  };
</script>

<style lang="scss">
  @use "@/styles/vars.scss" as *;

  .reporting-panel {
    &--dates {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 20px;
    }

    &--selected-range {
      display: flex;
      flex-wrap: wrap;
      justify-content: center;
      gap: 12px;
      color: $primary-text-color;
    }

    &--calendar-container {
      max-width: 100%;
      overflow-x: auto;
    }

    &--content {
      h4 {
        margin: 0;
      }
    }

    &--selection {
      padding: 16px 0 28px;
    }

    &--selection-intro {
      margin: 0 0 24px;
      color: $secondary-text-color;
      text-align: center;
    }

    &--report-selectors {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 20px;
      max-width: 840px;
      margin: 0 auto;
    }

    &--report-selector {
      display: grid;
      grid-template-columns: 44px minmax(0, 1fr);
      grid-template-rows: 1fr auto;
      column-gap: 18px;
      row-gap: 20px;
      min-width: 0;
      min-height: 220px;
      padding: 26px;
      border: 1px solid $secondary-grey;
      border-radius: 6px;
      background: $white;
      color: $primary-text-color;
      font: inherit;
      text-align: left;
      cursor: pointer;
      transition: border-color 0.2s, box-shadow 0.2s, background-color 0.2s;

      &:hover,
      &:focus-visible {
        border-color: $accent-primary-color;
        background-color: $primary-grey;
        box-shadow: 0 4px 16px rgba($primary-color, 0.1);
      }

      &:focus-visible {
        outline: 2px solid $accent-primary-color;
        outline-offset: 3px;
      }
    }

    &--report-icon {
      display: inline-flex;
      color: $secondary-color;
    }

    &--report-copy {
      display: flex;
      flex-direction: column;
      gap: 6px;
    }

    &--report-title {
      font-size: 18px;
      font-weight: 600;
    }

    &--report-description {
      color: $secondary-text-color;
      line-height: 1.5;
    }

    &--report-action {
      display: flex;
      grid-column: 2;
      gap: 8px;
      align-items: center;
      color: $secondary-color;
      font-weight: 600;
    }

    @media (max-width: $xs-screen) {
      &--report-selectors {
        grid-template-columns: 1fr;
      }

      &--report-selector {
        min-height: 0;
      }
    }
  }
</style>
