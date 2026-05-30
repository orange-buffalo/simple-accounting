<template>
  <SaPage>
    <template #header>
      <div class="sa-overview-page__header-row">
        <h1>{{ header }}</h1>

        <ElButton
          v-if="createActionVisible"
          class="sa-overview-page__create-action"
          link
          :disabled="createActionDisabled"
          @click="navigateToCreateAction"
        >
          <SaIcon icon="plus-thin" />
          {{ createActionLabel }}
        </ElButton>
      </div>
    </template>

    <div v-if="configuredFilters.length > 0" class="sa-overview-page__content-options">
      <div class="sa-overview-page__filters">
        <div
          v-if="activeFilterTags.length > 0"
          class="sa-overview-page__active-filters"
        >
          <ElTag
            v-for="tag in activeFilterTags"
            :key="tag.key"
            closable
            @close="removeActiveFilter(tag)"
          >
            {{ tag.label }}: {{ tag.valueLabel }}
          </ElTag>
        </div>

        <ElPopover
          v-model:visible="filterPopoverVisible"
          placement="bottom-start"
          trigger="click"
          :width="462"
          popper-class="sa-overview-page__filters-popover"
        >
          <template #reference>
            <ElButton link class="sa-overview-page__filters-button">
              <Filter class="sa-overview-page__filters-button-icon" />
              {{ $t.overviewPage.filters.button() }}
            </ElButton>
          </template>

          <div class="sa-overview-page__filters-panel">
            <div class="sa-overview-page__filters-panel-header">
              <h2>{{ $t.overviewPage.filters.header(pendingFiltersCount) }}</h2>
              <ElButton
                v-if="pendingFiltersCount > 0"
                link
                type="danger"
                @click="clearPendingFilters"
              >
                {{ $t.overviewPage.filters.clearAll() }}
              </ElButton>
            </div>

            <div
              v-for="filter in configuredFilters"
              :key="filter.key"
              class="sa-overview-page__filter-control"
            >
              <label>{{ filter.config.label }}</label>
              <ElInput
                v-if="filter.config.type === 'text'"
                :model-value="getPendingTextFilterValue(filter.key)"
                clearable
                @update:model-value="setPendingTextFilterValue(filter.key, $event)"
              />
              <ElSelect
                v-if="filter.config.type === 'multi-select'"
                :model-value="getPendingFilterValue(filter.key)"
                multiple
                clearable
                collapse-tags
                collapse-tags-tooltip
                :teleported="false"
                :placeholder="$t.overviewPage.filters.selectPlaceholder()"
                @update:model-value="setPendingFilterValue(filter.key, $event)"
              >
                <ElOption
                  v-for="option in filter.config.options"
                  :key="String(option.value)"
                  :label="option.label"
                  :value="option.value"
                />
              </ElSelect>
            </div>

            <div class="sa-overview-page__filters-panel-actions">
              <ElButton type="primary" @click="applyFilters">
                {{ $t.overviewPage.filters.apply() }}
              </ElButton>
              <ElButton link @click="cancelFilters">
                {{ $t.common.cancel() }}
              </ElButton>
            </div>
          </div>
        </ElPopover>
      </div>

      <div class="sa-overview-page__actions">
        <slot name="actions" />
      </div>
    </div>

    <slot />
  </SaPage>
</template>

<script
  lang="ts"
  setup
  generic="TFilters extends SaOverviewFilters"
>
  import { computed, ref, watch } from 'vue';
  import { ElPopover, ElTag } from 'element-plus';
  import { Filter } from '@element-plus/icons-vue';
  import SaPage from '@/components/SaPage.vue';
  import SaIcon from '@/components/SaIcon.vue';
  import { $t } from '@/services/i18n';
  import useNavigation from '@/services/use-navigation';
  import type {
    SaOverviewFilterConfigs,
    SaOverviewFilters,
  } from '@/components/overview-page/overview-page-filters';

  type FilterKey = keyof TFilters & string;
  type ConfiguredFilter = {
    key: FilterKey,
    config: NonNullable<SaOverviewFilterConfigs<TFilters>[FilterKey]>,
  };
  type MultiSelectFilter = ConfiguredFilter & {
    config: Extract<ConfiguredFilter['config'], { type: 'multi-select' }>,
  };
  type ActiveFilterTag = {
    key: string,
    value?: unknown,
    label: string,
    valueLabel: string,
  };

  const props = withDefaults(defineProps<{
    header: string,
    modelValue?: TFilters,
    filters?: SaOverviewFilterConfigs<TFilters>,
    createActionAvailable?: boolean,
    createActionDisabled?: boolean,
    createActionLabel?: string,
    createActionViewName?: string,
  }>(), {
    createActionAvailable: true,
    createActionDisabled: false,
  });

  const emit = defineEmits<{
    'update:modelValue': [value: TFilters],
  }>();

  const { navigateByViewName } = useNavigation();
  const filterPopoverVisible = ref(false);
  const pendingFilters = ref<TFilters>({ ...props.modelValue } as TFilters);
  const createActionVisible = computed(() => props.createActionAvailable
    && props.createActionLabel != null
    && props.createActionViewName != null);
  const navigateToCreateAction = () => navigateByViewName(props.createActionViewName!);

  const configuredFilters = computed<ConfiguredFilter[]>(() => Object.entries(props.filters ?? {})
    .map(([key, config]) => ({
      key: key as FilterKey,
      config: config as ConfiguredFilter['config'],
    })));

  const createFiltersSnapshot = () => ({ ...props.modelValue } as TFilters);

  const emitFiltersUpdate = (partialFilters: Partial<TFilters>) => {
    emit('update:modelValue', {
      ...props.modelValue,
      ...partialFilters,
    } as TFilters);
  };

  watch(filterPopoverVisible, (visible) => {
    if (visible) {
      pendingFilters.value = createFiltersSnapshot();
    }
  });

  const getFilterValue = (filters: TFilters | undefined, key: string): unknown[] => {
    const value = filters?.[key as keyof TFilters];
    return Array.isArray(value) ? value : [];
  };

  const getPendingFilterValue = (key: string) => getFilterValue(pendingFilters.value, key);
  const getTextFilterValue = (filters: TFilters | undefined, key: string) => {
    const value = filters?.[key as keyof TFilters];
    return typeof value === 'string' ? value : '';
  };
  const getPendingTextFilterValue = (key: string) => getTextFilterValue(pendingFilters.value, key);

  const setPendingTextFilterValue = (key: string, value: string | undefined) => {
    pendingFilters.value = {
      ...pendingFilters.value,
      [key]: value || null,
    };
  };

  const setPendingFilterValue = (key: string, value: unknown) => {
    const nextValue = Array.isArray(value) && value.length > 0 ? value : null;
    pendingFilters.value = {
      ...pendingFilters.value,
      [key]: nextValue,
    };
  };

  const optionLabelByValue = (filter: MultiSelectFilter, value: unknown) => filter.config.options
    .find((option) => option.value === value)?.label ?? String(value);

  const activeFilterTags = computed<ActiveFilterTag[]>(() => {
    const tags: ActiveFilterTag[] = [];

    configuredFilters.value.forEach((filter) => {
      if (filter.config.type === 'text') {
        const value = getTextFilterValue(props.modelValue, filter.key);
        if (value) {
          tags.push({
            key: filter.key,
            label: filter.config.label,
            valueLabel: value,
          });
        }
        return;
      }

      const multiSelectFilter = filter as MultiSelectFilter;
      getFilterValue(props.modelValue, filter.key).forEach((value) => {
        tags.push({
          key: filter.key,
          value,
          label: filter.config.label,
          valueLabel: optionLabelByValue(multiSelectFilter, value),
        });
      });
    });

    return tags;
  });

  const pendingFiltersCount = computed(() => {
    let count = 0;
    configuredFilters.value.forEach((filter) => {
      count += filter.config.type === 'text'
        ? (getPendingTextFilterValue(filter.key) ? 1 : 0)
        : getPendingFilterValue(filter.key).length;
    });
    return count;
  });

  const removeActiveFilter = (tag: ActiveFilterTag) => {
    const filter = configuredFilters.value.find((candidate) => candidate.key === tag.key);
    if (filter?.config.type === 'text') {
      emitFiltersUpdate({ [tag.key]: null } as Partial<TFilters>);
      return;
    }

    const updatedValues = getFilterValue(props.modelValue, tag.key).filter((value) => value !== tag.value);
    emitFiltersUpdate({ [tag.key]: updatedValues.length > 0 ? updatedValues : null } as Partial<TFilters>);
  };

  const clearPendingFilters = () => {
    const clearedFilters: Record<string, null> = {};
    configuredFilters.value.forEach((filter) => {
      clearedFilters[filter.key] = null;
    });
    pendingFilters.value = {
      ...pendingFilters.value,
      ...clearedFilters,
    };
  };

  const applyFilters = () => {
    emit('update:modelValue', pendingFilters.value);
    filterPopoverVisible.value = false;
  };

  const cancelFilters = () => {
    pendingFilters.value = createFiltersSnapshot();
    filterPopoverVisible.value = false;
  };
</script>

<style lang="scss" scoped>
  @use "@/styles/vars.scss" as *;
  @use "@/styles/mixins.scss" as *;

  .sa-overview-page {
    &__header-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
      margin-bottom: 10px;

      h1 {
        margin: 0;
        font-size: 160%;
      }
    }

    &__create-action {
      :deep(span) {
        gap: 4px;
      }
    }

    &__content-options {
      display: flex;
      justify-content: flex-end;
      width: 100%;
      margin-bottom: 24px;
    }

    &__filters {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      justify-content: flex-end;
      gap: 8px;
      color: $secondary-text-color;
    }

    &__filters-button {
      justify-self: start;
      color: $secondary-text-color !important;

      &.el-button,
      &.el-button:hover,
      &.el-button:focus {
        --el-button-text-color: #{$secondary-text-color};
        --el-button-hover-text-color: #{$secondary-text-color};
        --el-button-active-text-color: #{$secondary-text-color};
      }

      :deep(span),
      :deep(svg) {
        color: $secondary-text-color;
      }
    }

    &__filters-button-icon {
      width: 16px;
      height: 16px;
      margin-right: 4px;
    }

    &__filters-panel-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 20px;

      h2 {
        margin: 0;
      }
    }

    &__filter-control {
      display: grid;
      grid-template-columns: 130px 1fr;
      align-items: center;
      gap: 12px;

      :deep(.el-input),
      :deep(.el-select) {
        width: 100%;
      }

      & + & {
        margin-top: 16px;
      }

      label {
        color: $secondary-text-color;
      }
    }

    &__active-filters {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;

      :deep(.el-tag) {
        --el-tag-bg-color: #{rgba($secondary-text-color, 0.08)};
        --el-tag-border-color: #{rgba($secondary-text-color, 0.18)};
        --el-tag-text-color: #{$secondary-text-color};

        .el-tag__close:hover {
          color: $secondary-text-color;
          background-color: rgba($secondary-text-color, 0.14);
        }
      }
    }

    &__filters-panel-actions {
      display: flex;
      justify-content: flex-start;
      gap: 20px;
      margin-top: 24px;
    }

    &__actions {
      justify-self: end;
    }

    &__filters-panel {
      padding: 8px;
    }
  }

  :global(.sa-overview-page__filters-popover.el-popper) {
    @include overlay-shadow;
  }
</style>
