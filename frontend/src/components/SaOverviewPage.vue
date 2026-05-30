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

    <template #header-options>
      <div class="sa-overview-page__header-options">
        <ElPopover
          v-model:visible="filterPopoverVisible"
          placement="bottom-start"
          trigger="click"
          :width="360"
          popper-class="sa-overview-page__filters-popover"
        >
          <template #reference>
            <ElButton link class="sa-overview-page__filters-button">
              <Filter class="sa-overview-page__filters-button-icon" />
              {{ $t.overviewPage.filters.button(activeFiltersCount) }}
            </ElButton>
          </template>

          <div class="sa-overview-page__filters-panel">
            <div class="sa-overview-page__filters-panel-header">
              <h2>{{ $t.overviewPage.filters.header(activeFiltersCount) }}</h2>
              <ElButton
                v-if="activeFiltersCount > 0"
                link
                type="danger"
                @click="clearAllFilters"
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
              <ElSelect
                v-if="filter.config.type === 'multi-select'"
                :model-value="getFilterValue(filter.key)"
                multiple
                clearable
                collapse-tags
                collapse-tags-tooltip
                :placeholder="$t.overviewPage.filters.selectPlaceholder()"
                @update:model-value="setFilterValue(filter.key, $event)"
              >
                <ElOption
                  v-for="option in filter.config.options"
                  :key="String(option.value)"
                  :label="option.label"
                  :value="option.value"
                />
              </ElSelect>
            </div>
          </div>
        </ElPopover>

        <div v-if="filterPlaceholder">
          <ElInput
            class="sa-overview-page__filter-input"
            :model-value="modelValue?.freeSearchText ?? ''"
            :placeholder="filterPlaceholder"
            clearable
            @update:model-value="updateFreeSearchText"
          >
            <template #prefix>
              <Search class="sa-overview-page__filter-input__icon" />
            </template>
          </ElInput>
        </div>

        <div class="sa-overview-page__actions">
          <slot name="actions" />
        </div>
      </div>
    </template>

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

    <slot />
  </SaPage>
</template>

<script
  lang="ts"
  setup
  generic="TFilters extends SaOverviewFilters"
>
  import { computed, ref } from 'vue';
  import { ElPopover, ElTag } from 'element-plus';
  import { Filter, Search } from '@element-plus/icons-vue';
  import SaPage from '@/components/SaPage.vue';
  import SaIcon from '@/components/SaIcon.vue';
  import { $t } from '@/services/i18n';
  import useNavigation from '@/services/use-navigation';
  import type {
    SaOverviewFilterConfigs,
    SaOverviewFilters,
  } from '@/components/overview-page/overview-page-filters';

  type FilterKey = Exclude<keyof TFilters, 'freeSearchText'> & string;
  type ConfiguredFilter = {
    key: FilterKey,
    config: NonNullable<SaOverviewFilterConfigs<TFilters>[FilterKey]>,
  };
  type ActiveFilterTag = {
    key: string,
    value?: unknown,
    label: string,
    valueLabel: string,
  };

  const props = withDefaults(defineProps<{
    header: string,
    filterPlaceholder?: string,
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
  const createActionVisible = computed(() => props.createActionAvailable
    && props.createActionLabel != null
    && props.createActionViewName != null);
  const navigateToCreateAction = () => navigateByViewName(props.createActionViewName!);

  const configuredFilters = computed<ConfiguredFilter[]>(() => Object.entries(props.filters ?? {})
    .map(([key, config]) => ({
      key: key as FilterKey,
      config: config as ConfiguredFilter['config'],
    })));

  const emitFiltersUpdate = (partialFilters: Partial<TFilters>) => {
    emit('update:modelValue', {
      ...props.modelValue,
      ...partialFilters,
    } as TFilters);
  };

  const updateFreeSearchText = (value: string | undefined) => {
    emitFiltersUpdate({
      freeSearchText: value || null,
    } as Partial<TFilters>);
  };

  const getFilterValue = (key: string): unknown[] => {
    const value = props.modelValue?.[key as keyof TFilters];
    return Array.isArray(value) ? value : [];
  };

  const setFilterValue = (key: string, value: unknown) => {
    const nextValue = Array.isArray(value) && value.length > 0 ? value : null;
    emitFiltersUpdate({ [key]: nextValue } as Partial<TFilters>);
  };

  const optionLabelByValue = (filter: ConfiguredFilter, value: unknown) => filter.config.options
    .find((option) => option.value === value)?.label ?? String(value);

  const activeFilterTags = computed<ActiveFilterTag[]>(() => {
    const tags: ActiveFilterTag[] = [];

    if (props.modelValue?.freeSearchText) {
      tags.push({
        key: 'freeSearchText',
        label: $t.value.overviewPage.filters.freeSearchText.label(),
        valueLabel: props.modelValue.freeSearchText,
      });
    }

    configuredFilters.value.forEach((filter) => {
      getFilterValue(filter.key).forEach((value) => {
        tags.push({
          key: filter.key,
          value,
          label: filter.config.label,
          valueLabel: optionLabelByValue(filter, value),
        });
      });
    });

    return tags;
  });

  const activeFiltersCount = computed(() => activeFilterTags.value.length);

  const removeActiveFilter = (tag: ActiveFilterTag) => {
    if (tag.key === 'freeSearchText') {
      updateFreeSearchText(undefined);
      return;
    }

    const updatedValues = getFilterValue(tag.key).filter((value) => value !== tag.value);
    setFilterValue(tag.key, updatedValues);
  };

  const clearAllFilters = () => {
    const clearedFilters: Record<string, null> = { freeSearchText: null };
    configuredFilters.value.forEach((filter) => {
      clearedFilters[filter.key] = null;
    });
    emitFiltersUpdate(clearedFilters as Partial<TFilters>);
    filterPopoverVisible.value = false;
  };
</script>

<style lang="scss" scoped>
  @use "@/styles/vars.scss" as *;

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

    &__header-options {
      display: grid;
      grid-template-columns: 1fr minmax(260px, min(75%, 800px)) 1fr;
      align-items: center;
      width: 100%;
    }

    &__filters-button {
      justify-self: start;
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
      grid-template-columns: 110px 1fr;
      align-items: center;
      gap: 12px;

      label {
        color: $secondary-text-color;
      }
    }

    &__active-filters {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      margin-top: -20px;
      margin-bottom: 20px;
    }

    &__actions {
      justify-self: end;
    }

    &__filters-panel {
      padding: 8px;
    }

    &__filter-input {
      width: 100%;

      :deep(.el-input__wrapper) {
        background-color: transparent;
        border: none;
        box-shadow: none;
        color: $secondary-text-color;
      }

      &__icon {
        width: 16px;
        height: 16px;
      }
    }
  }
</style>
