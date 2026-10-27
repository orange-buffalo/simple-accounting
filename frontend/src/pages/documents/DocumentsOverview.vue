<template>
  <SaOverviewPage
    v-model="overviewFilters"
    :header="$t.documentsOverview.header()"
    :filters="documentFilters"
    :create-action-label="$t.documentsOverview.create()"
    create-action-view-name="create-standalone-document"
    :create-action-available="isCurrentUserRegular()"
    :create-action-disabled="!currentWorkspace.editable"
  >
    <template #filters>
      <div class="document-filters">
        <div class="document-filters__chips">
          <div
            v-for="filter in filterChips"
            :key="filter.key"
            v-click-outside="(event: MouseEvent) => closeOutsideEditor(filter.key, event)"
            class="document-filters__chip"
            :class="{ 'document-filters__chip--active': filter.active }"
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
              popper-class="document-filter-popover"
              @after-enter="focusEditor(filter.key)"
            >
              <template #reference>
                <button
                  :id="`document-filter-trigger-${filter.key}`"
                  type="button"
                  :aria-expanded="editingFilter === filter.key"
                  :aria-controls="`document-filter-editor-${filter.key}`"
                  @click="editingFilter = editingFilter === filter.key ? null : filter.key"
                >
                  <span>{{ filter.label }}:</span>
                  <strong v-if="!filter.active">{{ $t.documentsOverview.filters.any() }}</strong>
                  <ArrowDown aria-hidden="true" />
                </button>
              </template>
              <div
                v-if="editingFilter === filter.key"
                :id="`document-filter-editor-${filter.key}`"
                class="document-filters__editor"
                @keydown.esc.prevent.stop="closeEditor(filter.key)"
              >
                <label v-if="filter.key === 'freeSearchText'" for="document-filter-search">
                  {{ documentFilters.freeSearchText.label }}
                </label>
                <ElInput
                  v-if="filter.key === 'freeSearchText'"
                  id="document-filter-search"
                  :model-value="overviewFilters.freeSearchText ?? ''"
                  clearable
                  @update:model-value="overviewFilters.freeSearchText = $event || null"
                />
                <ElCheckboxGroup
                  v-else
                  :model-value="overviewFilters[filter.key] ?? []"
                  :aria-label="documentFilters[filter.key].label"
                  @update:model-value="setSelectedValues(filter.key, $event)"
                >
                  <ElCheckbox
                    v-for="option in documentFilters[filter.key].options"
                    :key="option.value"
                    :value="option.value"
                  >
                    {{ option.label }}
                  </ElCheckbox>
                </ElCheckboxGroup>
                <ElButton link @click="closeEditor(filter.key)">
                  {{ $t.documentsOverview.filters.closeEditor() }}
                </ElButton>
              </div>
            </ElPopover>
            <span v-for="value in filter.values" :key="value.value" class="document-filters__value">
              <strong :title="value.label">{{ value.label }}</strong>
              <button
                type="button"
                :aria-label="$t.documentsOverview.filters.clearValue(filter.label, value.label)"
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
    <SaPageableItems
      ref="pageItems"
      :page-query="documentsPageQuery"
      path="workspace.documents"
      :page-query-arguments="{
        workspaceId: currentWorkspaceId,
        freeSearchText: overviewFilters.freeSearchText,
        storageIdsIn: overviewFilters.storageIdsIn,
        usageTypeIn: overviewFilters.usageTypeIn,
      }"
      #default="{ item }"
    >
      <DocumentsOverviewPanel
        :document="item"
        @deleted="reloadDocuments"
      />
    </SaPageableItems>
  </SaOverviewPage>
</template>

<script lang="ts" setup>
  import { computed, nextTick, ref } from 'vue';
  import { ClickOutside as vClickOutside, ElCheckboxGroup, ElPopover } from 'element-plus';
  import { ArrowDown, Close } from '@element-plus/icons-vue';
  import SaOverviewPage from '@/components/SaOverviewPage.vue';
  import SaPageableItems from '@/components/pageable-items/SaPageableItems.vue';
  import DocumentsOverviewPanel from '@/pages/documents/DocumentsOverviewPanel.vue';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import { graphql } from '@/services/api/gql';
  import { $t } from '@/services/i18n';
  import { useAuth } from '@/services/api';
  import { DocumentUsageFilterType } from '@/services/api/gql/schema-types';
  import {
    createOverviewFilters,
    type SaOverviewFilterConfigs,
  } from '@/components/overview-page/overview-page-filters';

  type DocumentsOverviewFilters = {
    freeSearchText: string | null,
    storageIdsIn: string[] | null,
    usageTypeIn: DocumentUsageFilterType[] | null,
  };

  const { currentWorkspaceId, currentWorkspace } = useCurrentWorkspace();
  const { isCurrentUserRegular } = useAuth();
  const pageItems = ref<{ reload: () => void }>();

  const reloadDocuments = () => pageItems.value?.reload();

  const overviewFilters = ref(createOverviewFilters<DocumentsOverviewFilters>({
    freeSearchText: null,
    storageIdsIn: null,
    usageTypeIn: null,
  }));

  const documentFilters = {
    freeSearchText: {
      type: 'text',
      label: $t.value.documentsOverview.filters.freeSearchText.label(),
    },
    usageTypeIn: {
      type: 'multi-select',
      label: $t.value.documentsOverview.filters.usage.label(),
      options: [
        {
          label: $t.value.documentsOverview.filters.usage.expense(),
          value: DocumentUsageFilterType.Expense,
        },
        {
          label: $t.value.documentsOverview.filters.usage.income(),
          value: DocumentUsageFilterType.Income,
        },
        {
          label: $t.value.documentsOverview.filters.usage.invoice(),
          value: DocumentUsageFilterType.Invoice,
        },
        {
          label: $t.value.documentsOverview.filters.usage.incomeTaxPayment(),
          value: DocumentUsageFilterType.IncomeTaxPayment,
        },
        {
          label: $t.value.documentsOverview.filters.usage.standaloneDocument(),
          value: DocumentUsageFilterType.StandaloneDocument,
        },
        {
          label: $t.value.documentsOverview.filters.usage.unused(),
          value: DocumentUsageFilterType.Unused,
        },
      ],
    },
    storageIdsIn: {
      type: 'multi-select',
      label: $t.value.documentsOverview.filters.storage.label(),
      options: [
        {
          label: $t.value.documentsOverviewPanel.storage.googleDrive(),
          value: 'google-drive',
        },
        {
          label: $t.value.documentsOverviewPanel.storage.internalSystem(),
          value: 'local-fs',
        },
        {
          label: $t.value.documentsOverviewPanel.storage.unknown(),
          value: 'noop',
        },
      ],
    },
  } satisfies SaOverviewFilterConfigs<DocumentsOverviewFilters>;

  type FilterKey = keyof DocumentsOverviewFilters;
  const editingFilter = ref<FilterKey | null>(null);
  const closeOutsideEditor = (key: FilterKey, event: MouseEvent) => {
    if (editingFilter.value === key
      && event.target instanceof Node
      && !document.getElementById(`document-filter-editor-${key}`)?.contains(event.target)) {
      editingFilter.value = null;
    }
  };
  const filterChips = computed(() => Object.entries(documentFilters).map(([key, config]) => {
    const filterKey = key as FilterKey;
    const value = overviewFilters.value[filterKey];
    const search = overviewFilters.value.freeSearchText;
    const values = config.type === 'text'
      ? (search ? [{ value: search, label: search }] : [])
      : config.options.filter((option) => Array.isArray(value) && value.some((item) => item === option.value))
        .map((option) => ({ value: option.value, label: option.label }));
    return {
      key: filterKey,
      label: config.type === 'text' ? $t.value.documentsOverview.filters.search() : config.label,
      values,
      active: values.length > 0,
    };
  }));
  const hasActiveFilters = computed(() => filterChips.value.some((filter) => filter.active));
  const setSelectedValues = (key: FilterKey, values: unknown[]) => {
    overviewFilters.value = { ...overviewFilters.value, [key]: values.length ? values : null };
  };
  const removeValue = (key: FilterKey, value: string) => {
    if (key === 'freeSearchText') {
      overviewFilters.value.freeSearchText = null;
    } else {
      setSelectedValues(key, (overviewFilters.value[key] ?? []).filter((item) => item !== value));
    }
  };
  const focusEditor = async (key: FilterKey) => {
    await nextTick();
    document.getElementById(`document-filter-editor-${key}`)?.querySelector('input')?.focus();
  };
  const closeEditor = (key: FilterKey) => {
    editingFilter.value = null;
    document.getElementById(`document-filter-trigger-${key}`)?.focus();
  };
  const clearFilters = () => {
    overviewFilters.value = createOverviewFilters<DocumentsOverviewFilters>({
      freeSearchText: null,
      storageIdsIn: null,
      usageTypeIn: null,
    });
  };

  const documentsPageQuery = graphql(`
    query documentsPage(
      $workspaceId: String!,
      $first: Int!,
      $after: String,
      $freeSearchText: String,
      $storageIdsIn: [String!],
      $usageTypeIn: [DocumentUsageFilterType!]
    ) {
      workspace(id: $workspaceId) {
        documents(
          first: $first,
          after: $after,
          freeSearchText: $freeSearchText,
          storageIdsIn: $storageIdsIn,
          usageTypeIn: $usageTypeIn
        ) {
          edges {
            cursor
            node {
              id
              name
              timeUploaded
              storageId
              usedBy {
                type
                relatedEntityId
                displayName
              }
            }
          }
          pageInfo {
            ...PaginationPageInfo
          }
          totalCount
        }
      }
    }
  `);
</script>

<style lang="scss" scoped>
  @use '@/styles/vars.scss' as *;

  .document-filters {
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
      padding-right: 20px;
      border: 1px solid $components-border-color;
      border-radius: $status-border-radius;
      background: $white;
      color: $secondary-text-color;

      &--active {
        color: $components-color;
        background: $primary-grey;
      }

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

      > button svg { position: absolute; right: 6px; top: 7px; }
      strong { font-weight: 600; max-width: 240px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
      svg { width: 10px; height: 10px; flex-shrink: 0; }
    }

    &__value {
      display: inline-flex;
      align-items: center;
      gap: 2px;
      button { padding: 2px; }
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
      .el-button { font-size: inherit; }
      .el-checkbox-group { display: flex; flex-direction: column; gap: 2px; }
      :deep(.el-checkbox) { margin-right: 0; height: 22px; --el-checkbox-font-size: 11px; }
      :deep(.el-checkbox__inner) { width: 12px; height: 12px; }
      :deep(.el-checkbox__inner::after) { left: 3px; top: 0; }
    }
  }
</style>

<style lang="scss">
  .document-filter-popover.el-popover {
    padding: 10px;
    font-size: 11px;
    .el-popover__title { font-size: inherit; margin-bottom: 8px; }
  }
</style>
