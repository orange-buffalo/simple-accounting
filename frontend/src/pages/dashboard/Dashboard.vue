<template>
  <div class="sa-dashboard">
    <div class="sa-page-header">
      <h1>{{ $t.dashboard.header() }}</h1>

      <div class="sa-header-options">
        <span>&nbsp;</span>

        <!--suppress HtmlDeprecatedAttribute -->
        <!-- TODO translations -->
        <ElDatePicker
          v-model="selectedDateRange"
          type="daterange"
          align="right"
          unlink-panels
          range-separator="To"
          start-placeholder="Start date"
          end-placeholder="End date"
        />
      </div>
    </div>

    <div class="sa-dashboard__row">
      <DashboardCardExpenses :from-date="selectedFromDate"
                             :to-date="selectedToDate"
      />
      <DashboardCardIncomes :from-date="selectedFromDate"
                            :to-date="selectedToDate"
      />
      <DashboardCardProfit :from-date="selectedFromDate"
                           :to-date="selectedToDate"
      />
      <DashboardInvoices :from-date="selectedFromDate"
                         :to-date="selectedToDate"
      />
    </div>
  </div>
</template>

<script lang="ts" setup>
  import { ref, computed, watch } from 'vue';
  import { useStorage } from '@/services/storage';
  import DashboardCardExpenses from '@/pages/dashboard/DashboardCardExpenses.vue';
  import DashboardCardIncomes from '@/pages/dashboard/DashboardCardIncomes.vue';
  import DashboardCardProfit from '@/pages/dashboard/DashboardCardProfit.vue';
  import DashboardInvoices from '@/pages/dashboard/DashboardInvoices.vue';
  import { $t } from '@/services/i18n';

  const storage = useStorage<Array<Date>>('dashboard.selected-date-range');

  const selectedDateRange = ref<Array<Date>>(storage.getOrDefault([]));
  if (!selectedDateRange.value.length) {
    const now = new Date();
    selectedDateRange.value = [
      new Date(now.getFullYear(), 0, 1),
      now,
    ];
  } else {
    selectedDateRange.value = selectedDateRange.value.map((it) => new Date(it));
  }

  watch(selectedDateRange, (newDatesRanges) => {
    storage.set(newDatesRanges);
  });

  const selectedFromDate = computed(() => (selectedDateRange.value[0]));
  const selectedToDate = computed(() => (selectedDateRange.value[1]));
</script>

<style lang="scss">
  @use "@/styles/vars.scss" as *;

  .el-date-editor {
    max-width: 250px;
  }

  .sa-dashboard__row {
    display: flex;
    justify-content: space-evenly;
    align-items: stretch;
    flex-wrap: wrap;
  }
</style>
