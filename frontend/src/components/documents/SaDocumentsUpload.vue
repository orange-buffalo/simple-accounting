<template>
  <div class="sa-documents-upload">
    <div
      v-if="uiState.initialLoading"
      class="sa-documents-upload__loading-placeholder"
    />

    <SaFailedDocumentsStorageMessage v-else-if="!uiState.storageActive" />

    <template v-else-if="uiState.documentsLoading">
      <SaDocument
        v-for="documentId in documentsIds"
        :key="documentId"
        :loading="true"
        class="sa-documents-upload__document"
      />
    </template>

    <SaDocumentUpload
      v-for="documentAggregate in documentsAggregates"
      v-else
      :key="documentAggregate.key"
      ref="uploadControls"
      :document-id="documentAggregate.document.id"
      :document-name="documentAggregate.document.name"
      :document-size-in-bytes="documentAggregate.document.sizeInBytes"
      class="sa-documents-upload__document"
      @upload-completed="documentAggregate.onUploadComplete($event)"
      @upload-failed="documentAggregate.onUploadFailure()"
      @document-selected="documentAggregate.onFileSelection()"
      @document-removed="onDocumentRemoval(documentAggregate.key)"
    />
  </div>
</template>

<script>
  import { computed, ref, watch } from '@vue/composition-api';
  import SaDocumentUpload from '@/components/documents/SaDocumentUpload';
  import { api } from '@/services/api';
  import SaDocument from '@/components/documents/SaDocument';
  import useCurrentWorkspace from '@/components/workspace/useCurrentWorkspace';
  import useDocumentsStorageStatus from '@/components/documents/storage/useDocumentsStorageStatus';
  import SaFailedDocumentsStorageMessage from '@/components/documents/storage/SaFailedDocumentsStorageMessage';

  const DOCUMENT_AGGREGATE_STATE = {
    EMPTY: 'empty',
    PENDING: 'pending',
    UPLOAD_FAILED: 'upload-failed',
    UPLOAD_COMPLETED: 'upload-completed',
  };

  class DocumentAggregate {
    constructor({ document, onDocumentAggregateChange }) {
      this.document = document || {};
      this.key = document != null ? document.id.toString() : new Date().getMilliseconds()
        .toString();
      this.state = document != null ? DOCUMENT_AGGREGATE_STATE.UPLOAD_COMPLETED : DOCUMENT_AGGREGATE_STATE.EMPTY;
      this.onDocumentAggregateChange = onDocumentAggregateChange;
    }

    onUploadComplete(document) {
      this.document = document;
      this.state = DOCUMENT_AGGREGATE_STATE.UPLOAD_COMPLETED;
      this.onDocumentAggregateChange();
    }

    onUploadFailure() {
      this.state = DOCUMENT_AGGREGATE_STATE.UPLOAD_FAILED;
      this.onDocumentAggregateChange();
    }

    onFileSelection() {
      this.state = DOCUMENT_AGGREGATE_STATE.PENDING;
      this.onDocumentAggregateChange();
    }
  }

  function useDocumentsAggregates({ emit, documents, props }) {
    const documentsAggregates = ref([]);

    function addEmptyDocumentAggregateIfNecessary() {
      const emptyUpload = documentsAggregates.value
        .find((it) => it.state === DOCUMENT_AGGREGATE_STATE.EMPTY);
      if (!emptyUpload) {
        documentsAggregates.value.push(new DocumentAggregate({ onDocumentAggregateChange }));
      }
    }

    function onDocumentRemoval(documentAggregateKey) {
      documentsAggregates.value = documentsAggregates.value.filter((it) => it.key !== documentAggregateKey);
    }

    function onDocumentAggregateChange() {
      addEmptyDocumentAggregateIfNecessary();

      const pendingUpload = documentsAggregates.value
        .find((it) => it.state === DOCUMENT_AGGREGATE_STATE.PENDING);
      if (pendingUpload) {
        return;
      }

      const failedUpload = documentsAggregates.value
        .find((it) => it.state === DOCUMENT_AGGREGATE_STATE.UPLOAD_FAILED);
      if (failedUpload) {
        emit('uploads-failed');
        return;
      }

      const documentsIds = documentsAggregates.value
        .filter((it) => it.state === DOCUMENT_AGGREGATE_STATE.UPLOAD_COMPLETED)
        .map((it) => it.document.id);
      emit('uploads-completed', documentsIds);
    }

    watch(documents, () => {
      documentsAggregates.value = documents.value.map((document) => new DocumentAggregate({
        document,
        onDocumentAggregateChange,
      }));

      addEmptyDocumentAggregateIfNecessary();
    }, { lazy: true });

    watch(() => props.documentsIds, async () => {
      if (!props.documentsIds.length) {
        documentsAggregates.value = [];
      }
      addEmptyDocumentAggregateIfNecessary();
    });

    addEmptyDocumentAggregateIfNecessary();

    return {
      documentsAggregates,
      onDocumentRemoval,
      onDocumentAggregateChange,
    };
  }

  function useUploadControls(onDocumentAggregateChange) {
    const uploadControls = ref(null);
    const submitUploads = function submitUploads() {
      if (uploadControls.value != null) {
        uploadControls.value.forEach((upload) => upload.submitUpload());
      }
      onDocumentAggregateChange();
    };
    return {
      uploadControls,
      submitUploads,
    };
  }

  function useDocumentsApi(props) {
    const documents = ref([]);
    const documentsLoading = ref(false);

    async function loadDocuments() {
      documentsLoading.value = true;
      try {
        const { currentWorkspaceId } = useCurrentWorkspace();
        documents.value = await api.pageRequest(`/workspaces/${currentWorkspaceId}/documents`)
          .eager()
          .inFilter('id', props.documentsIds)
          .getPageData();
      } finally {
        documentsLoading.value = false;
      }
    }

    watch(() => props.documentsIds, async () => {
      if (props.documentsIds.length) {
        await loadDocuments();
      }
    }, { lazy: false });

    return {
      documents,
      documentsLoading,
    };
  }

  function useUiState({ documentsStorageStatus, documentsLoading, props }) {
    const documentsReassigned = ref(false);
    watch(() => props.documentsIds, () => {
      documentsReassigned.value = true;
    }, { lazy: true });

    const uiState = computed(() => {
      const state = {
        initialLoading: false,
        documentsLoading: false,
        storageActive: true,
      };

      if (documentsStorageStatus.value.loading) {
        state.initialLoading = true;
      } else if (!documentsStorageStatus.value.active) {
        state.storageActive = false;
      } else if (props.loadingOnCreate && !props.documentsIds.length && !documentsReassigned.value) {
        state.initialLoading = true;
      } else if (documentsLoading.value) {
        state.documentsLoading = true;
      }

      return state;
    });

    return {
      uiState,
    };
  }

  export default {
    components: {
      SaFailedDocumentsStorageMessage,
      SaDocument,
      SaDocumentUpload,
    },

    props: {
      documentsIds: {
        type: Array,
        required: true,
      },
      loadingOnCreate: {
        type: Boolean,
        default: false,
      },
    },

    setup(props, { emit }) {
      const { documents, documentsLoading } = useDocumentsApi(props);

      const {
        documentsAggregates, onDocumentAggregateChange, onDocumentRemoval,
      } = useDocumentsAggregates({
        emit,
        props,
        documents,
      });

      const { documentsStorageStatus } = useDocumentsStorageStatus();

      return {
        documentsAggregates,
        onDocumentRemoval,
        documentsStorageStatus,
        ...useUploadControls(onDocumentAggregateChange),
        ...useUiState({
          documentsLoading,
          documentsStorageStatus,
          props,
        }),
      };
    },
  };
</script>

<style lang="scss">
  @import "~@/styles/mixins.scss";

  .sa-documents-upload {
    &__document {
      margin-bottom: 15px;
      width: 100%;
    }

    &__loading-placeholder {
      height: 80px;
      @include loading-placeholder;
    }
  }
</style>
