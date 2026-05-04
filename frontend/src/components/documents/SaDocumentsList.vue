<template>
  <div class="sa-documents-list">
    <div
      v-if="downloadStoragesLoading"
      class="sa-documents-list__loading-placeholder"
    />

    <SaFailedDocumentsStorageMessage
      v-else-if="hasUnsupportedStorages"
    />

    <template v-else>
      <SaDocument
        v-for="document in sortedDocuments"
        :key="document.id"
        :document-name="document.name"
        :document-id="document.id"
        :document-size-in-bytes="document.sizeInBytes ?? undefined"
        class="sa-documents-list__document"
      />
    </template>
  </div>
</template>

<script lang="ts" setup>
  import { computed } from 'vue';
  import SaDocument from '@/components/documents/SaDocument.vue';
  import SaFailedDocumentsStorageMessage from '@/components/documents/storage/SaFailedDocumentsStorageMessage.vue';
  import { useFragment } from '@/services/api/gql/fragment-masking';
  import { DocumentDataFragment, type DocumentDataFragmentType } from '@/components/documents/documents-gql-types';
  import { useDownloadDocumentStoragesStatus } from '@/components/documents/storage/useDocumentsStorageStatus';

  const props = defineProps<{ documents: ReadonlyArray<DocumentDataFragmentType> }>();

  const { downloadStoragesStatus } = useDownloadDocumentStoragesStatus();

  const resolvedDocuments = computed(
    () => props.documents.map((d) => useFragment(DocumentDataFragment, d)),
  );

  const sortedDocuments = computed(
    () => [...resolvedDocuments.value].sort((a, b) => a.name.localeCompare(b.name)),
  );

  const hasUnsupportedStorages = computed(() => {
    if (downloadStoragesStatus.value.loading) return false;
    return resolvedDocuments.value.some(
      (doc) => !downloadStoragesStatus.value.ids.has(doc.storageId),
    );
  });

  const downloadStoragesLoading = computed(
    () => downloadStoragesStatus.value.loading || !downloadStoragesStatus.value.loaded,
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
