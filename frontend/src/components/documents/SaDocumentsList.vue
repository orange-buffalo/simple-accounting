<template>
  <div class="sa-documents-list">
    <div
      v-if="downloadStoragesLoading"
      class="sa-documents-list__loading-placeholder"
    />

    <template v-else-if="documentsLoading">
      <SaDocument
        v-for="documentId in documentsIds"
        :key="documentId"
        :loading="true"
        class="sa-documents-list__document"
      />
    </template>

    <SaFailedDocumentsStorageMessage
      v-else-if="hasUnsupportedStorages"
    />

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
  import { useCurrentWorkspace } from '@/services/workspaces';
  import type { DocumentDto } from '@/services/api';
  import { consumeAllPages, documentsApi, useRequestConfig } from '@/services/api';
  import { graphql } from '@/services/api/gql';
  import { useQuery } from '@/services/api/use-gql-api';

  const props = defineProps<{ documentsIds: number[] }>();

  const documents = ref<DocumentDto[]>([]);
  const documentsLoading = ref(false);
  const hasUnsupportedStorages = ref(false);

  const [downloadStoragesLoading, downloadStoragesData] = useQuery(graphql(/* GraphQL */ `
    query downloadDocumentStorages {
      getDownloadDocumentStorages {
        id
      }
    }
  `), 'getDownloadDocumentStorages');

  watch(() => [props.documentsIds, downloadStoragesLoading.value], async (_, __, onCleanup) => {
    if (downloadStoragesLoading.value) {
      return;
    }
    if (props.documentsIds.length) {
      documentsLoading.value = true;

      const {
        requestConfig,
        cancelRequest,
      } = useRequestConfig({});

      onCleanup(cancelRequest);

      const { currentWorkspaceId } = useCurrentWorkspace();

      try {
        const loadedDocuments = (await consumeAllPages((pageRequest) => documentsApi.getDocuments({
          workspaceId: currentWorkspaceId,
          ...pageRequest,
          idIn: props.documentsIds,
        }, requestConfig)))
          .sort((a, b) => a.name.localeCompare(b.name));

        const availableStorageIds = new Set(
          (downloadStoragesData.value ?? []).map((s) => s.id),
        );
        hasUnsupportedStorages.value = loadedDocuments.some(
          (doc) => !availableStorageIds.has(doc.storageId),
        );

        documents.value = loadedDocuments;
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
