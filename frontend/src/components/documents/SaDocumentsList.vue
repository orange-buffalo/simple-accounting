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
        :document-size-in-bytes="document.sizeInBytes"
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
  import { graphql } from '@/services/api/gql';
  import { useQuery } from '@/services/api/use-gql-api';
  import { DocumentDataFragment, type DocumentDataFragmentType } from '@/components/documents/documents-gql-types';

  const props = defineProps<{ documents: ReadonlyArray<DocumentDataFragmentType> }>();

  const [downloadStoragesLoading, downloadStoragesData] = useQuery(graphql(/* GraphQL */ `
    query downloadDocumentStorages {
      getDownloadDocumentStorages {
        id
      }
    }
  `), 'getDownloadDocumentStorages');

  const resolvedDocuments = computed(
    () => props.documents.map((d) => useFragment(DocumentDataFragment, d)),
  );

  const sortedDocuments = computed(
    () => [...resolvedDocuments.value].sort((a, b) => a.name.localeCompare(b.name)),
  );

  const hasUnsupportedStorages = computed(() => {
    if (downloadStoragesLoading.value) return false;
    const availableStorageIds = new Set(
      (downloadStoragesData.value ?? []).map((s) => s.id),
    );
    return resolvedDocuments.value.some(
      (doc) => !availableStorageIds.has(doc.storageId),
    );
  });
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
