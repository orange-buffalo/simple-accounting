<template>
  <SaOverviewItem :title="document.name">
    <template #primary-attributes>
      <SaOverviewItemPrimaryAttribute
        :tooltip="$t.documentsOverviewPanel.timeUploaded.label()"
        icon="calendar"
      >
        {{ $t.common.dateTime.medium(document.timeUploaded) }}
      </SaOverviewItemPrimaryAttribute>

      <SaOverviewItemPrimaryAttribute
        :tooltip="$t.documentsOverviewPanel.storage.label()"
        icon="upload"
      >
        {{ storageLabel }}
      </SaOverviewItemPrimaryAttribute>

      <SaOverviewItemPrimaryAttribute
        v-for="usage in document.usedBy"
        :key="`${usage.type}-${usage.relatedEntityId}`"
        :tooltip="usageTooltip(usage)"
        :icon="usageIcon(usage.type)"
      >
        <ElButton
          link
          class="documents-overview-panel__usage-link"
          @click="navigateToUsage(usage)"
        >
          {{ usage.displayName }}
        </ElButton>
      </SaOverviewItemPrimaryAttribute>
    </template>

    <template #last-column>
      <div class="documents-overview-panel__actions">
        <SaDocumentDownloadLink
          :document-id="document.id"
          :document-name="document.name"
          :disabled="storageActionDisabled"
          :disabled-tooltip="storageActionDisabledTooltip"
          show-icon
          class="documents-overview-panel__download-link"
        />
        <ElDropdown
          v-if="canDelete"
          trigger="click"
          @command="handleActionCommand"
        >
          <span>
            <ElTooltip
              :content="storageActionDisabledTooltip"
              :disabled="!storageActionDisabledTooltip"
              placement="bottom"
            >
              <span>
                <ElButton
                  link
                  :aria-label="$t.documentsOverviewPanel.actions.label()"
                  :disabled="storageActionDisabled || deleting"
                >
                  <SaIcon icon="menu" />
                </ElButton>
              </span>
            </ElTooltip>
          </span>

          <template #dropdown>
            <ElDropdownMenu>
              <ElDropdownItem
                command="delete"
                :disabled="deleting"
                class="documents-overview-panel__danger-action"
              >
                <SaIcon
                  icon="delete"
                  class="documents-overview-panel__action-icon"
                />
                {{ $t.documentsOverviewPanel.delete.label() }}
              </ElDropdownItem>
            </ElDropdownMenu>
          </template>
        </ElDropdown>
      </div>
    </template>
  </SaOverviewItem>
</template>

<script lang="ts" setup>
  import { computed, ref } from 'vue';
  import { ElDropdown, ElDropdownItem, ElDropdownMenu } from 'element-plus';
  import SaIcon from '@/components/SaIcon.vue';
  import SaOverviewItem from '@/components/overview-item/SaOverviewItem.vue';
  import SaOverviewItemPrimaryAttribute from '@/components/overview-item/SaOverviewItemPrimaryAttribute.vue';
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

  const canDelete = computed(() => props.document.usedBy.length === 0);

  const deleteDocument = useConfirmation(
    $t.value.documentsOverviewPanel.delete.confirm.message(),
    {
      title: $t.value.documentsOverviewPanel.delete.confirm.title(),
      confirmButtonText: $t.value.documentsOverviewPanel.delete.confirm.yes(),
      cancelButtonText: $t.value.documentsOverviewPanel.delete.confirm.no(),
      type: 'warning',
    },
    async () => {
      if (storageActionDisabled.value) return;
      deleting.value = true;
      try {
        await deleteDocumentMutation({
          workspaceId: currentWorkspaceId,
          documentId: props.document.id,
        });
        emit('deleted');
      } finally {
        deleting.value = false;
      }
    },
  );

  const handleActionCommand = (command: string) => {
    if (command === 'delete') {
      deleteDocument();
    }
  };

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
    STANDALONE_DOCUMENT: 'documents-overview',
  };

  const usageTypeToIconMap: Record<DocumentUsageType, string> = {
    EXPENSE: 'expense',
    INCOME: 'income',
    INVOICE: 'invoices-overview',
    INCOME_TAX_PAYMENT: 'income-tax-payments-overview',
    STANDALONE_DOCUMENT: 'documents-overview',
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

  const usageTooltip = (usage: DocumentUsage) => (
    $t.value.documentsOverviewPanel.usage.navigateTooltip(usageLabel(usage.type))
  );

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
    &__usage-link {
      padding: 0;
      height: auto;
    }

    &__actions {
      display: flex;
      gap: 12px;
      justify-content: flex-end;
      flex-wrap: wrap;
    }

    &__action-icon {
      margin-right: 4px;
    }

    &__danger-action {
      color: var(--el-color-danger);
    }
  }
</style>
