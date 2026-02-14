<template>
  <div class="sa-documents-list">
    <div
      v-if="documentsStorageStatus.loading"
      class="sa-documents-list__loading-placeholder"
    />

    <SaFailedDocumentsStorageMessage v-else-if="!documentsStorageStatus.active" />

    <template v-else-if="documentsLoading">
      <SaDocument
        v-for="documentId in documentsIds"
        :key="documentId"
        :loading="true"
        class="sa-documents-list__document"
      />
    </template>

    <template v-else>
      <SaDocument
        v-for="document in documents"
        :key="document.id"
        :document-name="document.name"
        :document-id="document.id"
        :document-size-in-bytes="document.sizeInBytes"
        class="sa-documents-list__document"
      />
    </template>
  </div>
</template>

<script lang="ts" setup>
import { ref, watch } from 'vue';
import SaDocument from '@/components/documents/SaDocument.vue';
import SaFailedDocumentsStorageMessage from '@/components/documents/storage/SaFailedDocumentsStorageMessage.vue';
import useDocumentsStorageStatus from '@/components/documents/storage/useDocumentsStorageStatus';
import type { DocumentDto } from '@/services/api';
import { consumeAllPages, documentsApi, useRequestConfig } from '@/services/api';
import { useCurrentWorkspace } from '@/services/workspaces';

const props = defineProps<{ documentsIds: number[] }>();

const documents = ref<DocumentDto[]>([]);
const documentsLoading = ref(false);
const { documentsStorageStatus } = useDocumentsStorageStatus();

watch(
  () => [props.documentsIds, documentsStorageStatus.value],
  async (_, __, onCleanup) => {
    if (documentsStorageStatus.value.loading || !documentsStorageStatus.value.active) {
      return;
    }
    if (props.documentsIds.length) {
      documentsLoading.value = true;

      const { requestConfig, cancelRequest } = useRequestConfig({});

      onCleanup(cancelRequest);

      const { currentWorkspaceId } = useCurrentWorkspace();

      try {
        documents.value = (
          await consumeAllPages((pageRequest) =>
            documentsApi.getDocuments(
              {
                workspaceId: currentWorkspaceId,
                ...pageRequest,
                idIn: props.documentsIds,
              },
              requestConfig,
            ),
          )
        ).sort((a, b) => a.name.localeCompare(b.name));
      } finally {
        documentsLoading.value = false;
      }
    }
  },
  { immediate: true },
);
</script>

<style lang="scss">
  @use "@/styles/mixins.scss" as *;

  .sa-documents-list {
    &__document {
      margin-bottom: 15px;

      &:last-child {
        margin-bottom: 0;
      }
    }

    &__loading-placeholder {
      height: 50px;
      @include loading-placeholder;
    }
  }
</style>
