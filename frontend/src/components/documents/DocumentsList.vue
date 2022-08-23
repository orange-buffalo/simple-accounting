<template>
  <div class="sa-documents-list">
    <div
      v-if="documentsStorageStatus.loading"
      class="sa-documents-list__loading-placeholder"
    />

    <FailedDocumentsStorageMessage v-else-if="!documentsStorageStatus.active" />

    <template v-else-if="documentsLoading">
      <Document
        v-for="documentId in documentsIds"
        :key="documentId"
        :loading="true"
        class="sa-documents-list__document"
      />
    </template>

    <template v-else>
      <Document
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
  import Document from '@/components/documents/Document.vue';
  import useDocumentsStorageStatus from '@/components/documents/storage/useDocumentsStorageStatus';
  import FailedDocumentsStorageMessage from '@/components/documents/storage/FailedDocumentsStorageMessage.vue';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import type { DocumentDto } from '@/services/api';
  import { consumeAllPages, documentsApi, useCancellableRequest } from '@/services/api';

  const props = defineProps<{ documentsIds: number[] }>();

  const documents = ref<DocumentDto[]>([]);
  const documentsLoading = ref(false);
  const { documentsStorageStatus } = useDocumentsStorageStatus();

  watch(() => props.documentsIds, async (documentsIds, _, onCleanup) => {
    if (documentsIds.length) {
      documentsLoading.value = true;

      const {
        cancellableRequestConfig,
        cancelRequest,
      } = useCancellableRequest();

      onCleanup(cancelRequest);

      const { currentWorkspaceId } = useCurrentWorkspace();

      try {
        documents.value = await consumeAllPages((pageRequest) => documentsApi.getDocuments({
          workspaceId: currentWorkspaceId,
          ...pageRequest,
          idIn: props.documentsIds,
        }, cancellableRequestConfig));
      } finally {
        documentsLoading.value = false;
      }
    }
  }, { immediate: true });
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
