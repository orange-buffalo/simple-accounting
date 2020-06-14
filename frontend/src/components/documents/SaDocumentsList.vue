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

<script>
  import { ref, watch } from '@vue/composition-api';
  import { api } from '@/services/api';
  import SaDocument from '@/components/documents/SaDocument';
  import useCurrentWorkspace from '@/components/workspace/useCurrentWorkspace';
  import useDocumentsStorageStatus from '@/components/documents/storage/useDocumentsStorageStatus';
  import SaFailedDocumentsStorageMessage from '@/components/documents/storage/SaFailedDocumentsStorageMessage';

  export default {
    components: {
      SaFailedDocumentsStorageMessage,
      SaDocument,
    },

    props: {
      documentsIds: {
        type: Array,
        required: true,
      },
    },

    setup(props) {
      const documents = ref([]);
      const documentsLoading = ref(false);
      const { documentsStorageStatus } = useDocumentsStorageStatus();

      watch(() => props.documentsIds, async (documentsIds, _, onCleanup) => {
        if (documentsIds.length) {
          documentsLoading.value = true;

          const cancelToken = api.createCancelToken();
          onCleanup(() => cancelToken.cancel());

          const { currentWorkspaceApiUrl } = useCurrentWorkspace();

          try {
            documents.value = await api.pageRequest(currentWorkspaceApiUrl('documents'))
              .eager()
              .inFilter('id', documentsIds)
              .config({
                cancelToken: cancelToken.token,
              })
              .getPageData();
          } finally {
            documentsLoading.value = false;
          }
        }
      });

      return {
        documents,
        documentsLoading,
        documentsStorageStatus,
      };
    },
  };
</script>

<style lang="scss">
  @import "~@/styles/mixins.scss";

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
