<template>
  <SaOverviewItem :title="document.name">
    <template #primary-attributes>
      <SaOverviewItemPrimaryAttribute
        v-for="usage in document.usedBy"
        :key="`${usage.type}-${usage.relatedEntityId}`"
        :tooltip="usageLabel(usage.type)"
        :icon="usageIcon(usage.type)"
      >
        {{ usage.displayName }}
      </SaOverviewItemPrimaryAttribute>
    </template>

    <template #attributes-preview>
      <SaOverviewItemAttributePreviewIcon
        icon="calendar"
        :tooltip="$t.documentsOverviewPanel.timeUploaded.tooltip($t.common.dateTime.medium(document.timeUploaded))"
      />
      <ElTooltip
        :content="storageLabel"
        placement="bottom"
      >
        <SaIcon
          v-if="document.storageId === 'google-drive'"
          icon="google-drive"
          class="overview-item-attribute-preview-icon"
        />
        <Folder
          v-else-if="document.storageId === 'local-fs'"
          data-icon="folder"
          class="sa-icon overview-item-attribute-preview-icon documents-overview-panel__element-plus-preview-icon"
        />
        <DataLine
          v-else
          data-icon="data-line"
          class="sa-icon overview-item-attribute-preview-icon documents-overview-panel__element-plus-preview-icon"
        />
      </ElTooltip>
    </template>

    <template #last-column>
      <div class="documents-overview-panel__actions">
        <ElPopover
          trigger="click"
          placement="bottom-end"
          popper-class="documents-overview-panel__actions-popover"
        >
          <template #reference>
            <ElButton
              link
              :icon="Menu"
              :aria-label="$t.documentsOverviewPanel.actions.label()"
              class="documents-overview-panel__actions-trigger"
            />
          </template>

          <div class="documents-overview-panel__actions-menu">
            <SaDocumentDownloadLink
              :document-id="document.id"
              :document-name="document.name"
              :disabled="storageActionDisabled"
              :disabled-tooltip="storageActionDisabledTooltip"
              show-icon
              class="documents-overview-panel__action"
            />
            <ElButton
              v-for="usage in document.usedBy"
              :key="`${usage.type}-${usage.relatedEntityId}`"
              link
              class="documents-overview-panel__action"
              @click="navigateToUsage(usage)"
            >
              <SaIcon
                :icon="usageIcon(usage.type)"
                class="documents-overview-panel__action-icon"
              />
              {{ $t.documentsOverviewPanel.navigate.label(usage.displayName) }}
            </ElButton>
            <ElTooltip
              v-if="canDelete"
              :content="deleteDisabledTooltip"
              :disabled="!deleteDisabledTooltip"
              placement="bottom"
            >
              <span>
                <ElButton
                  link
                  type="danger"
                  :icon="Delete"
                  :disabled="deleteDisabled"
                  class="documents-overview-panel__action documents-overview-panel__danger-action"
                  @click="deleteDocument"
                >
                  {{ $t.documentsOverviewPanel.delete.label() }}
                </ElButton>
              </span>
            </ElTooltip>
          </div>
        </ElPopover>
      </div>
    </template>
  </SaOverviewItem>
</template>

<script lang="ts" setup>
  import { computed, ref } from 'vue';
  import { ElPopover, ElTooltip } from 'element-plus';
  import { DataLine, Delete, Folder, Menu } from '@element-plus/icons-vue';
  import SaOverviewItem from '@/components/overview-item/SaOverviewItem.vue';
  import SaOverviewItemPrimaryAttribute from '@/components/overview-item/SaOverviewItemPrimaryAttribute.vue';
  import SaOverviewItemAttributePreviewIcon from '@/components/overview-item/SaOverviewItemAttributePreviewIcon.vue';
  import SaIcon from '@/components/SaIcon.vue';
  import SaDocumentDownloadLink from '@/components/documents/SaDocumentDownloadLink.vue';
  import { useConfirmation } from '@/components/confirmation/use-confirmation';
  import { useDownloadDocumentStoragesStatus } from '@/components/documents/storage/useDocumentsStorageStatus';
  import useNavigation from '@/services/use-navigation';
  import { $t } from '@/services/i18n';
  import { graphql } from '@/services/api/gql';
  import { useMutation } from '@/services/api/use-gql-api';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import type { DocumentUsageType } from '@/services/api/gql/graphql';

  const deleteDocumentMutation = useMutation(graphql(/* GraphQL */ `
    mutation deleteDocument($workspaceId: String!, $documentId: String!) {
      deleteDocument(workspaceId: $workspaceId, documentId: $documentId)
    }
  `), 'deleteDocument');

  const removeStandaloneDocumentMutation = useMutation(graphql(/* GraphQL */ `
    mutation removeStandaloneDocument(
      $workspaceId: String!,
      $standaloneDocumentId: String!,
      $removeDocumentIfUnused: Boolean
    ) {
      removeStandaloneDocument(
        workspaceId: $workspaceId,
        standaloneDocumentId: $standaloneDocumentId,
        removeDocumentIfUnused: $removeDocumentIfUnused
      )
    }
  `), 'removeStandaloneDocument');

  interface DocumentUsage {
    type: DocumentUsageType,
    relatedEntityId: string,
    displayName: string,
  }

  interface DocumentItem {
    id: string,
    name: string,
    timeUploaded: string,
    storageId: string,
    usedBy: DocumentUsage[],
  }

  const props = defineProps<{
    document: DocumentItem,
  }>();

  const emit = defineEmits<{
    deleted: [],
  }>();

  const { navigateToView } = useNavigation();
  const { currentWorkspaceId } = useCurrentWorkspace();
  const { downloadStoragesStatus } = useDownloadDocumentStoragesStatus();
  const deleting = ref(false);

  const downloadStoragesLoading = computed(
    () => downloadStoragesStatus.value.loading || !downloadStoragesStatus.value.loaded,
  );

  const documentStorageAvailable = computed(
    () => downloadStoragesStatus.value.ids.has(props.document.storageId),
  );

  const storageActionDisabled = computed(
    () => downloadStoragesLoading.value || !documentStorageAvailable.value,
  );

  const storageActionDisabledTooltip = computed(() => {
    if (downloadStoragesLoading.value) {
      return $t.value.documentsOverviewPanel.download.waitingForStorage();
    }
    if (!documentStorageAvailable.value) {
      return $t.value.documentsOverviewPanel.download.storageInactive();
    }
    return undefined;
  });

  const standaloneDocumentUsage = computed(() => {
    if (props.document.usedBy.length !== 1) return undefined;

    const usage = props.document.usedBy[0];
    return usage.type === 'STANDALONE_DOCUMENT' ? usage : undefined;
  });

  const canDelete = computed(() => props.document.usedBy.length === 0 || standaloneDocumentUsage.value != null);

  const deleteDisabled = computed(() => deleting.value || (
    standaloneDocumentUsage.value == null && storageActionDisabled.value
  ));

  const deleteDisabledTooltip = computed(() => (
    standaloneDocumentUsage.value == null ? storageActionDisabledTooltip.value : undefined
  ));

  const deleteDocument = useConfirmation(
    $t.value.documentsOverviewPanel.delete.confirm.message(),
    {
      title: $t.value.documentsOverviewPanel.delete.confirm.title(),
      confirmButtonText: $t.value.documentsOverviewPanel.delete.confirm.yes(),
      cancelButtonText: $t.value.documentsOverviewPanel.delete.confirm.no(),
      type: 'warning',
    },
    async () => {
      if (deleteDisabled.value) return;
      deleting.value = true;
      try {
        if (standaloneDocumentUsage.value) {
          await removeStandaloneDocumentMutation({
            workspaceId: currentWorkspaceId,
            standaloneDocumentId: standaloneDocumentUsage.value.relatedEntityId,
            removeDocumentIfUnused: true,
          });
        } else {
          await deleteDocumentMutation({
            workspaceId: currentWorkspaceId,
            documentId: props.document.id,
          });
        }
        emit('deleted');
      } finally {
        deleting.value = false;
      }
    },
  );

  const storageLabel = computed(() => {
    switch (props.document.storageId) {
    case 'google-drive':
      return $t.value.documentsOverviewPanel.storage.googleDrive();
    case 'local-fs':
      return $t.value.documentsOverviewPanel.storage.internalSystem();
    default:
      return $t.value.documentsOverviewPanel.storage.unknown();
    }
  });

  const usageTypeToRouteMap: Record<DocumentUsageType, string> = {
    EXPENSE: 'edit-expense',
    INCOME: 'edit-income',
    INVOICE: 'edit-invoice',
    INCOME_TAX_PAYMENT: 'edit-income-tax-payment',
    STANDALONE_DOCUMENT: 'edit-standalone-document',
  };

  const usageTypeToIconMap: Record<DocumentUsageType, string> = {
    EXPENSE: 'expense',
    INCOME: 'income',
    INVOICE: 'invoices-overview',
    INCOME_TAX_PAYMENT: 'income-tax-payments-overview',
    STANDALONE_DOCUMENT: 'attachment',
  };

  const usageIcon = (usageType: DocumentUsageType) => usageTypeToIconMap[usageType];

  const usageLabel = (usageType: DocumentUsageType) => {
    switch (usageType) {
    case 'EXPENSE':
      return $t.value.documentsOverviewPanel.usage.expense();
    case 'INCOME':
      return $t.value.documentsOverviewPanel.usage.income();
    case 'INVOICE':
      return $t.value.documentsOverviewPanel.usage.invoice();
    case 'INCOME_TAX_PAYMENT':
      return $t.value.documentsOverviewPanel.usage.incomeTaxPayment();
    case 'STANDALONE_DOCUMENT':
      return $t.value.documentsOverviewPanel.usage.standaloneDocument();
    }
  };

  const navigateToUsage = (usage: DocumentUsage) => {
    const routeName = usageTypeToRouteMap[usage.type];
    navigateToView({
      name: routeName,
      params: { id: usage.relatedEntityId },
    });
  };

</script>

<style lang="scss">
  .documents-overview-panel {
    &__actions {
      display: flex;
      justify-content: flex-end;
    }

    &__danger-action {
      color: var(--el-color-danger);
    }

    &__action-icon {
      margin-right: 4px;
    }

    &__element-plus-preview-icon {
      color: var(--el-text-color-regular);
      margin-right: 5px;
      width: 18px;
      height: 18px;
    }
  }

  .documents-overview-panel__actions-popover {
    width: max-content !important;
    min-width: 220px !important;
    max-width: calc(100vw - 32px) !important;
  }

  .documents-overview-panel__actions-menu {
    display: inline-flex;
    flex-direction: column;
    align-items: stretch;
    gap: 5px;
    min-width: 220px;
    width: max-content;
    max-width: calc(100vw - 64px);

    .documents-overview-panel__action,
    .sa-document-download-link,
    .el-button {
      width: 100%;
    }

    .el-button {
      justify-content: flex-start;
      margin-left: 0;
      white-space: nowrap;
    }
  }
</style>
