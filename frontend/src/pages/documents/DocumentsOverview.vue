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
            class="document-filters__chip"
            :class="{ 'document-filters__chip--active': filter.active }"
          >
            <button
              type="button"
              :aria-expanded="editingFilter === filter.key"
              aria-controls="document-filter-editor"
              @click="editingFilter = editingFilter === filter.key ? null : filter.key"
            >
              <span>{{ filter.label }}:</span>
              <strong>{{ filter.summary }}</strong>
              <ArrowDown aria-hidden="true" />
            </button>
            <button
              v-if="filter.active"
              type="button"
              :aria-label="$t.documentsOverview.filters.clear(filter.label)"
              @click="clearFilter(filter.key)"
            >
              <Close aria-hidden="true" />
            </button>
          </div>
          <ElButton v-if="hasActiveFilters" link @click="clearFilters">
            {{ $t.overviewPage.filters.clearAll() }}
          </ElButton>
        </div>
        <div
          v-if="editingFilter"
          id="document-filter-editor"
          class="document-filters__editor"
          @keydown.esc="editingFilter = null"
        >
          <label v-if="editingFilter === 'freeSearchText'" for="document-filter-search">
            {{ documentFilters.freeSearchText.label }}
          </label>
          <ElInput
            v-if="editingFilter === 'freeSearchText'"
            id="document-filter-search"
            :model-value="overviewFilters.freeSearchText ?? ''"
            clearable
            @update:model-value="overviewFilters.freeSearchText = $event || null"
          />
          <ElCheckboxGroup
            v-else
            :model-value="overviewFilters[editingFilter] ?? []"
            :aria-label="documentFilters[editingFilter].label"
            @update:model-value="setSelectedValues(editingFilter, $event)"
          >
            <ElCheckbox
              v-for="option in documentFilters[editingFilter].options"
              :key="option.value"
              :value="option.value"
            >
              {{ option.label }}
            </ElCheckbox>
          </ElCheckboxGroup>
          <ElButton link @click="editingFilter = null">
            {{ $t.documentsOverview.filters.closeEditor() }}
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
  import { computed, ref } from 'vue';
  import { ElCheckboxGroup } from 'element-plus';
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
  const filterChips = computed(() => Object.entries(documentFilters).map(([key, config]) => {
    const filterKey = key as FilterKey;
    const value = overviewFilters.value[filterKey];
    const summary = config.type === 'text'
      ? overviewFilters.value.freeSearchText
      : config.options.filter((option) => Array.isArray(value) && value.some((item) => item === option.value))
        .map((option) => option.label).join(', ');
    return {
      key: filterKey,
      label: config.type === 'text' ? $t.value.documentsOverview.filters.search() : config.label,
      summary: summary || $t.value.documentsOverview.filters.any(),
      active: Boolean(summary),
    };
  }));
  const hasActiveFilters = computed(() => filterChips.value.some((filter) => filter.active));
  const clearFilter = (key: FilterKey) => {
    overviewFilters.value = { ...overviewFilters.value, [key]: null };
  };
  const clearFilters = () => {
    overviewFilters.value = createOverviewFilters<DocumentsOverviewFilters>({
      freeSearchText: null,
      storageIdsIn: null,
      usageTypeIn: null,
    });
  };
  const setSelectedValues = (key: FilterKey, values: unknown[]) => {
    overviewFilters.value = { ...overviewFilters.value, [key]: values.length ? values : null };
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

    &__chips {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 12px;
    }

    &__chip {
      display: inline-flex;
      align-items: center;
      border: 1px solid $components-border-color;
      border-radius: $status-border-radius;
      background: $white;
      color: $secondary-text-color;
      overflow: hidden;

      &--active {
        color: $components-color;
        background: $success-color-bg;
        border-color: $success-color-lighter-iii;
      }

      button {
        display: inline-flex;
        align-items: center;
        gap: 8px;
        padding: 10px 14px;
        border: 0;
        background: transparent;
        color: inherit;
        font: inherit;
        cursor: pointer;

        &:hover { background: rgba($components-color, 0.06); }
        &:focus-visible { outline: 2px solid $components-color; outline-offset: -3px; }
        & + button { padding-inline: 10px; border-left: 1px solid $success-color-lighter-iii; }
      }

      strong { font-weight: 600; max-width: 420px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
      svg { width: 14px; height: 14px; flex-shrink: 0; }
    }

    &__editor {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 16px;
      margin-top: 16px;
      padding: 16px 20px;
      background: $white;
      border: 1px solid $components-border-color;
      border-radius: $components-border-radius;

      label { color: $secondary-text-color; }
      .el-input { width: 360px; }
      .el-checkbox-group { display: flex; flex-wrap: wrap; gap: 8px 24px; }
      :deep(.el-checkbox) { margin-right: 0; }
    }
  }
</style>
