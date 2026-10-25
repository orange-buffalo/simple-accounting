<template>
  <SaPage :header="$t.reporting.header()">

    <SaWizard v-model="activeWizardStep" :steps="wizardSteps" :loading="reportGenerationInProgress" class="reporting-panel">
      <template #select-report>
        <div class="reporting-panel--content reporting-panel--selection">
          <p class="reporting-panel--selection-intro">
            {{ $t.reporting.wizard.selectionIntro() }}
          </p>
          <div class="reporting-panel--report-selectors">
            <button
              type="button"
              class="reporting-panel--report-selector"
              :aria-pressed="selectedReport === GENERAL_TAX_REPORT"
              @click="selectReport(GENERAL_TAX_REPORT)"
            >
              <span class="reporting-panel--report-icon" aria-hidden="true"><SaIcon icon="tax" :size="42" /></span>
              <span class="reporting-panel--report-copy">
                <span class="reporting-panel--report-title">{{ $t.reporting.wizard.reports.generalTax.title() }}</span>
                <span class="reporting-panel--report-description">{{ $t.reporting.wizard.reports.generalTax.description() }}</span>
              </span>
              <span class="reporting-panel--report-action" aria-hidden="true">{{ $t.reporting.wizard.buttons.select() }}</span>
            </button>
            <button
              type="button"
              class="reporting-panel--report-selector"
              :aria-pressed="selectedReport === INCOME_TAX_REPORT"
              @click="selectReport(INCOME_TAX_REPORT)"
            >
              <span class="reporting-panel--report-icon" aria-hidden="true"><SaIcon icon="reporting" :size="42" /></span>
              <span class="reporting-panel--report-copy">
                <span class="reporting-panel--report-title">{{ $t.reporting.wizard.reports.incomeTax.title() }}</span>
                <span class="reporting-panel--report-description">{{ $t.reporting.wizard.reports.incomeTax.description() }}</span>
              </span>
              <span class="reporting-panel--report-action" aria-hidden="true">{{ $t.reporting.wizard.buttons.select() }}</span>
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
            @report-loaded="reportGenerated = true"
            @report-settled="reportGenerationInProgress = false"
          />
          <IncomeTaxReport
            v-if="selectedReport === INCOME_TAX_REPORT"
            :date-range="selectedDateRange"
            @report-loaded="reportGenerated = true"
            @report-settled="reportGenerationInProgress = false"
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

  const steps = { SELECT_REPORT: 'select-report', SELECT_DATES: 'select-dates', VIEW_REPORT: 'view-report' } as const;
  type ReportStep = typeof steps[keyof typeof steps];

  const GENERAL_TAX_REPORT = 'generalTaxReport';
  const INCOME_TAX_REPORT = 'incomeTaxReport';
  type Report = typeof GENERAL_TAX_REPORT | typeof INCOME_TAX_REPORT;

  const activeWizardStep = ref<ReportStep>(steps.SELECT_REPORT);
  const selectedDateRange = ref<Array<Date>>([]);
  const rangeSelectionInProgress = ref(false);
  const pendingDateRange = ref<Array<Date | null> | null>(null);
  const displayedDateRange = computed(() => pendingDateRange.value ?? selectedDateRange.value);
  const selectedReport = ref<Report>();
  const reportGenerationInProgress = ref(false);
  const reportGenerated = ref(false);

  const reportSelectionActive = computed(() => activeWizardStep.value === steps.SELECT_REPORT);

  const datesSelectionActive = computed(() => activeWizardStep.value === steps.SELECT_DATES);

  const viewReportActive = computed(() => activeWizardStep.value === steps.VIEW_REPORT);

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

  const viewReportStepDescription = computed(() => {
    if (viewReportActive.value && reportGenerationInProgress.value) {
      return $t.value.reporting.wizard.steps.viewReport.description.loading();
    }
    if (viewReportActive.value && reportGenerated.value) {
      return $t.value.reporting.wizard.steps.viewReport.description.ready();
    }
    return null;
  });

  const wizardSteps = computed<WizardStep<ReportStep>[]>(() => [
    {
      id: steps.SELECT_REPORT,
      title: $t.value.reporting.wizard.steps.selectReport.title(),
      description: reportSelectionStepDescription.value,
      complete: !!selectedReport.value,
      next: { target: steps.SELECT_DATES, disabled: !selectedReport.value },
    },
    {
      id: steps.SELECT_DATES,
      title: $t.value.reporting.wizard.steps.selectDates.title(),
      description: datesSelectionStepDescription.value,
      complete: selectedDateRange.value.length === 2 && !rangeSelectionInProgress.value,
      next: {
        target: steps.VIEW_REPORT,
        disabled: selectedDateRange.value.length !== 2 || rangeSelectionInProgress.value,
      },
    },
    {
      id: steps.VIEW_REPORT,
      title: $t.value.reporting.wizard.steps.viewReport.title(),
      description: viewReportStepDescription.value,
      complete: reportGenerated.value,
      onEnter: () => {
        reportGenerated.value = false;
        reportGenerationInProgress.value = true;
      },
    },
  ]);

  const onCalendarChange = (dates: Array<Date | null>) => {
    reportGenerated.value = false;
    rangeSelectionInProgress.value = dates.length !== 2 || dates.some((date) => date == null);
    pendingDateRange.value = rangeSelectionInProgress.value ? dates : null;
  };

  const onRangeSelected = () => {
    rangeSelectionInProgress.value = false;
    pendingDateRange.value = null;
  };

  const selectReport = (report: Report) => {
    reportGenerated.value = false;
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
      &[aria-pressed="true"],
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
