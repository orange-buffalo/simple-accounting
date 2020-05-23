<template>
  <div class="sa-documents-list">
    <template v-if="loading">
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

  export default {
    components: { SaDocument },

    props: {
      documentsIds: {
        type: Array,
        required: true,
      },
    },

    setup(props) {
      const documents = ref([]);
      const loading = ref(false);

      watch(() => props.documentsIds, async (documentsIds, _, onCleanup) => {
        if (documentsIds.length) {
          loading.value = true;

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
            loading.value = false;
          }
        }
      });

      return {
        documents,
        loading,
      };
    },
  };
</script>

<style lang="scss">
  .sa-documents-list {
    &__document {
      margin-bottom: 15px;

      &:last-child {
        margin-bottom: 0;
      }
    }
  }
</style>
