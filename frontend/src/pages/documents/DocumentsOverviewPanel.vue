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
    </template>

    <template #middle-column>
      <div
        v-if="document.usedBy.length > 0"
        class="documents-overview-panel__usages"
      >
        <ElButton
          v-for="usage in document.usedBy"
          :key="`${usage.type}-${usage.relatedEntityId}`"
          link
          @click="navigateToUsage(usage)"
        >
          {{ usage.displayName }}
        </ElButton>
      </div>
      <SaStatusLabel
        v-else
        status="pending"
      >
        {{ $t.documentsOverviewPanel.unused() }}
      </SaStatusLabel>
    </template>

    <template #last-column>
      <div class="documents-overview-panel__actions">
        <SaDocumentDownloadLink
          :document-id="document.id"
          :document-name="document.name"
          :disabled="storageActionDisabled"
          :disabled-tooltip="storageActionDisabledTooltip"
          class="documents-overview-panel__download-link"
        />
        <ElTooltip
          v-if="canDelete"
          :content="storageActionDisabledTooltip"
          :disabled="!storageActionDisabledTooltip"
          placement="bottom"
        >
          <span class="documents-overview-panel__delete-link">
            <ElButton
              link
              :disabled="storageActionDisabled || deleting"
              @click="deleteDocument"
            >
              {{ $t.documentsOverviewPanel.delete.label() }}
            </ElButton>
          </span>
        </ElTooltip>
      </div>
    </template>
  </SaOverviewItem>
</template>

<script lang="ts" setup>
  import { computed, ref } from 'vue';
  import SaOverviewItem from '@/components/overview-item/SaOverviewItem.vue';
  import SaOverviewItemPrimaryAttribute from '@/components/overview-item/SaOverviewItemPrimaryAttribute.vue';
  import SaStatusLabel from '@/components/SaStatusLabel.vue';
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
    &__usages {
      display: flex;
      flex-direction: column;
      align-items: center;
    }

    &__actions {
      display: flex;
      gap: 12px;
      justify-content: flex-end;
      flex-wrap: wrap;
    }
  }
</style>
