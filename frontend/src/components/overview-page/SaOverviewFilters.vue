<template>
  <div class="sa-overview-filters">
    <div class="sa-overview-filters__chips">
      <div
        v-for="filter in filterChips"
        :key="filter.key"
        v-click-outside="(event: MouseEvent) => closeOutsideEditor(filter.key, event)"
        class="sa-overview-filters__chip"
        :class="{ 'sa-overview-filters__chip--active': filter.active }"
      >
        <ElPopover
          :visible="editingFilter === filter.key"
          trigger="click"
          role="dialog"
          :title="filter.label"
          placement="bottom-start"
          :width="300"
          :persistent="false"
          :show-arrow="false"
          :show-after="0"
          :hide-after="0"
          transition=""
          popper-class="sa-overview-filter-popover"
          @after-enter="focusEditor(filter.key)"
        >
          <template #reference>
            <button
              :id="elementId('trigger', filter.key)"
              type="button"
              :aria-expanded="editingFilter === filter.key"
              :aria-controls="elementId('editor', filter.key)"
              @click="toggleEditor(filter.key)"
            >
              <span>{{ filter.label }}:</span>
              <strong v-if="!filter.active">{{ $t.overviewPage.filters.any() }}</strong>
            </button>
          </template>
          <div
            v-if="editingFilter === filter.key"
            :id="elementId('editor', filter.key)"
            class="sa-overview-filters__editor"
            @keydown.esc.prevent.stop="closeEditor(filter.key)"
          >
            <template v-if="filter.config.type === 'text'">
              <label :for="elementId('input', filter.key)">{{ filter.config.label }}</label>
              <ElInput
                :id="elementId('input', filter.key)"
                :model-value="textValue(filter.key)"
                clearable
                @update:model-value="updateValue(filter.key, $event || null)"
                @keydown.enter.prevent="closeEditor(filter.key)"
              />
            </template>
            <ElDatePicker
              v-else-if="filter.config.type === 'date-range'"
              :model-value="dateRangeValue(filter.key)"
              type="daterange"
              style="--el-date-editor-width: 100%; min-width: 0; box-sizing: border-box"
              value-format="YYYY-MM-DD"
              :teleported="false"
              :aria-label="filter.config.label"
              @update:model-value="updateValue(filter.key, $event || null)"
            />
            <div
              v-else-if="filter.config.type === 'multi-select'"
              class="sa-overview-filters__options"
              role="group"
              :aria-label="filter.config.label"
            >
              <ElCheckbox
                v-for="option in filter.config.options"
                :key="overviewFilterValueKey(option.value)"
                :model-value="selectedValues(filter.key).some((value) => value === option.value)"
                @update:model-value="toggleOption(filter.key, option.value, $event)"
              >
                {{ option.label }}
              </ElCheckbox>
            </div>
          </div>
        </ElPopover>
        <span v-for="value in filter.values" :key="overviewFilterValueKey(value.value)" class="sa-overview-filters__value">
          <strong :title="value.label">{{ value.label }}</strong>
          <button
            type="button"
            :aria-label="$t.overviewPage.filters.clearValue(filter.label, value.label)"
            @click.stop="removeValue(filter.key, value.value)"
          >
            <Close aria-hidden="true" />
          </button>
        </span>
      </div>
      <ElButton v-if="hasActiveFilters" link @click="clearFilters">
        {{ $t.overviewPage.filters.clearAll() }}
      </ElButton>
    </div>
  </div>
</template>

<script lang="ts" setup generic="TFilters extends SaOverviewFilters">
  import { computed, nextTick, shallowRef, useId } from 'vue';
  import { ClickOutside as vClickOutside, ElDatePicker, ElPopover } from 'element-plus';
  import { Close } from '@element-plus/icons-vue';
  import { $t } from '@/services/i18n';
  import type { SaOverviewFilterConfigs, SaOverviewFilters } from './overview-page-filters';
  import {
    clearOverviewFilterValues, overviewFilterValueKey, toggleOverviewFilterValue, updateOverviewFilterValue,
  } from './overview-page-filters';

  type FilterKey = keyof TFilters & string;
  type ConfiguredFilter = {
    key: FilterKey,
    config: NonNullable<SaOverviewFilterConfigs<TFilters>[FilterKey]>,
  };

  const props = defineProps<{
    modelValue?: TFilters,
    filters: SaOverviewFilterConfigs<TFilters>,
  }>();
  const emit = defineEmits<{ 'update:modelValue': [value: TFilters] }>();
  const instanceId = useId();
  const elementId = (part: string, key: FilterKey) => `${instanceId}-filter-${part}-${key}`;
  const editingFilter = shallowRef<FilterKey | null>(null);
  const toggleEditor = (key: FilterKey) => {
    editingFilter.value = editingFilter.value === key ? null : key;
  };
  const configuredFilters = computed<ConfiguredFilter[]>(() => Object.entries(props.filters)
    .map(([key, config]) => ({ key: key as FilterKey, config: config as ConfiguredFilter['config'] })));
  const textValue = (key: FilterKey): string => {
    const value = props.modelValue?.[key];
    return typeof value === 'string' ? value : '';
  };
  const selectedValues = (key: FilterKey): (string | number | boolean)[] => {
    const value = props.modelValue?.[key];
    return Array.isArray(value) ? value : [];
  };
  const dateRangeValue = (key: FilterKey): [string, string] | null => {
    const value = props.modelValue?.[key];
    return Array.isArray(value) && value.length === 2 ? [String(value[0]), String(value[1])] : null;
  };
  const filterChips = computed(() => configuredFilters.value.map(({ key, config }) => {
    const text = textValue(key);
    const range = dateRangeValue(key);
    const values = config.type === 'text'
      ? (text ? [{ value: text, label: text }] : [])
      : config.type === 'date-range'
        ? (range ? [{
          value: range.join('/'),
          label: range.map((date) => $t.value.common.date.medium(date)).join(' – '),
        }] : [])
        : config.options.filter((option) => selectedValues(key).some((value) => value === option.value));
    return {
      key,
      config,
      label: config.type === 'text' ? $t.value.overviewPage.filters.search() : config.label,
      values,
      active: values.length > 0,
    };
  }));
  const hasActiveFilters = computed(() => filterChips.value.some((filter) => filter.active));
  const updateValue = (key: FilterKey, value: unknown) => {
    emit('update:modelValue', updateOverviewFilterValue(props.modelValue, key, value));
  };
  const toggleOption = (key: FilterKey, value: string | number | boolean, checked: string | number | boolean) => {
    updateValue(key, toggleOverviewFilterValue(props.modelValue?.[key], value, Boolean(checked)));
  };
  const removeValue = (key: FilterKey, value: string | number | boolean) => {
    const filter = configuredFilters.value.find((candidate) => candidate.key === key);
    if (filter?.config.type === 'text' || filter?.config.type === 'date-range') {
      updateValue(key, null);
    } else {
      updateValue(key, toggleOverviewFilterValue(props.modelValue?.[key], value, false));
    }
  };
  const clearFilters = () => {
    emit('update:modelValue', clearOverviewFilterValues(props.modelValue, props.filters));
  };
  const closeOutsideEditor = (key: FilterKey, event: MouseEvent) => {
    if (editingFilter.value === key && event.target instanceof Node
      && !document.getElementById(elementId('editor', key))?.contains(event.target)) {
      editingFilter.value = null;
    }
  };
  const focusEditor = async (key: FilterKey) => {
    await nextTick();
    document.getElementById(elementId('editor', key))?.querySelector('input')?.focus();
  };
  const closeEditor = (key: FilterKey) => {
    editingFilter.value = null;
    document.getElementById(elementId('trigger', key))?.focus();
  };
</script>

<style lang="scss" scoped>
  @use '@/styles/vars.scss' as *;

  .sa-overview-filters {
    width: 100%;
    padding-inline: 16px;
    box-sizing: border-box;
    font-size: 11px;

    &__chips {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 8px;
      > .el-button { font-size: inherit; }
    }

    &__chip {
      display: inline-flex;
      position: relative;
      align-items: center;
      flex-wrap: wrap;
      gap: 3px;
      min-height: 24px;
      overflow: hidden;
      border: 1px solid $components-border-color;
      border-radius: $status-border-radius;
      background: $white;
      color: $secondary-text-color;

      &--active { color: $components-color; background: $primary-grey; }
      button {
        display: inline-flex;
        align-items: center;
        gap: 4px;
        padding: 4px 7px;
        border: 0;
        background: transparent;
        color: inherit;
        font: inherit;
        line-height: 14px;
        cursor: pointer;
        &:hover { background: rgba($components-color, 0.06); }
        &:focus-visible { outline: 2px solid $components-color; outline-offset: -3px; }
      }
      strong { font-weight: 600; max-width: 240px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
      svg { width: 10px; height: 10px; flex-shrink: 0; }
    }

    &__value {
      display: inline-flex;
      align-items: center;
      gap: 2px;
      button { padding: 2px; }
      &:last-child { padding-right: 4px; }
      & + & { border-left: 1px solid $components-border-color; padding-left: 4px; }
    }

    &__editor {
      display: flex;
      flex-direction: column;
      align-items: flex-start;
      gap: 8px;
      font-size: 11px;
      --el-font-size-base: 11px;
      label { color: $secondary-text-color; }
      .el-input { width: 100%; font-size: inherit; }
      .sa-overview-filters__options { display: flex; flex-direction: column; gap: 2px; }
      :deep(.el-checkbox) {
        margin-right: 0;
        height: 22px;
        --el-checkbox-font-size: 11px;
        --el-checkbox-border-radius: 3px;
        --el-checkbox-input-width: 12px;
        --el-checkbox-input-height: 12px;
      }
      :deep(.el-checkbox__inner::after) { width: 2px; height: 6px; }
    }
  }
</style>

<style lang="scss">
  .sa-overview-filter-popover.el-popover {
    padding: 10px;
    font-size: 11px;
    .el-popover__title { font-size: inherit; margin-bottom: 8px; }
  }
</style>
