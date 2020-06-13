<template>
  <div class="sa-documents-upload">
    <div
      v-if="loadingWithMinimumInfo"
      class="sa-documents-upload__loading-placeholder"
    />

    <template v-if="loadingWithDocumentsInfo">
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
      @upload-completed="onUploadComplete(documentAggregate.key, $event)"
      @upload-failed="onUploadFailure(documentAggregate.key)"
      @document-selected="onDocumentSelection(documentAggregate.key)"
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

  const DOCUMENT_AGGREGATE_STATE = {
    EMPTY: 'empty',
    PENDING: 'pending',
    UPLOAD_FAILED: 'upload-failed',
    UPLOAD_COMPLETED: 'upload-completed',
  };

  export default {
    components: {
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
      const documentsAggregates = ref([]);
      const loading = ref(props.loadingOnCreate);
      const loadingWithMinimumInfo = computed(() => loading.value && !props.documentsIds.length);
      const loadingWithDocumentsInfo = computed(() => loading.value && props.documentsIds.length);
      const { currentWorkspaceId } = useCurrentWorkspace();
      const uploadControls = ref(null);

      const addNewUploadControl = function addNewUploadControl() {
        documentsAggregates.value.push({
          document: {},
          key: new Date().getMilliseconds()
            .toString(),
          state: DOCUMENT_AGGREGATE_STATE.EMPTY,
        });
      };

      async function loadDocuments() {
        loading.value = true;
        try {
          const documents = await api.pageRequest(`/workspaces/${currentWorkspaceId}/documents`)
            .eager()
            .inFilter('id', props.documentsIds)
            .getPageData();

          documentsAggregates.value = documents.map(it => ({
            document: it,
            key: it.id.toString(),
            state: DOCUMENT_AGGREGATE_STATE.UPLOAD_COMPLETED,
          }));
        } finally {
          loading.value = false;
        }
        addNewUploadControl();
      }

      watch(() => props.documentsIds, async () => {
        if (!props.documentsIds.length) {
          documentsAggregates.value = [];
          loading.value = false;
        } else {
          await loadDocuments();
        }
      }, { lazy: true });

      const getDocumentAggregateByKey = function getDocumentAggregateByKey(key) {
        return documentsAggregates.value.find(it => it.key === key);
      };

      const onUploadComplete = function onUploadComplete(documentAggregateKey, uploadedDocument) {
        const documentAggregate = getDocumentAggregateByKey(documentAggregateKey);
        documentAggregate.state = DOCUMENT_AGGREGATE_STATE.UPLOAD_COMPLETED;
        documentAggregate.document = uploadedDocument;
        onUploadEvent();
      };

      const onUploadFailure = function onUploadFailure(documentAggregateKey) {
        const documentAggregate = getDocumentAggregateByKey(documentAggregateKey);
        documentAggregate.state = DOCUMENT_AGGREGATE_STATE.UPLOAD_FAILED;
        onUploadEvent();
      };

      const onDocumentSelection = function onDocumentSelection(documentAggregateKey) {
        const documentAggregate = getDocumentAggregateByKey(documentAggregateKey);
        documentAggregate.state = DOCUMENT_AGGREGATE_STATE.PENDING;
        addNewUploadControl();
      };

      const onDocumentRemoval = function onDocumentRemoval(documentAggregateKey) {
        documentsAggregates.value = documentsAggregates.value.filter(it => it.key !== documentAggregateKey);
      };

      const onUploadEvent = function onUploadEvent() {
        const pendingUploads = documentsAggregates.value
          .filter(it => it.state === DOCUMENT_AGGREGATE_STATE.PENDING);
        if (pendingUploads.length) {
          return;
        }

        const failedUploads = documentsAggregates.value
          .filter(it => it.state === DOCUMENT_AGGREGATE_STATE.UPLOAD_FAILED);
        if (failedUploads.length) {
          emit('uploads-failed');
          return;
        }

        const documentsIds = documentsAggregates.value
          .filter(it => it.state === DOCUMENT_AGGREGATE_STATE.UPLOAD_COMPLETED)
          .map(it => it.document.id);
        emit('uploads-completed', documentsIds);
      };

      const submitUploads = function submitUploads() {
        uploadControls.value.forEach(upload => upload.submitUpload());
        onUploadEvent();
      };

      if (!loading.value) {
        addNewUploadControl();
      }

      if (props.documentsIds.length) {
        loadDocuments();
      }

      return {
        documentsAggregates,
        loading,
        loadingWithMinimumInfo,
        loadingWithDocumentsInfo,
        getDocumentAggregateByKey,
        onUploadComplete,
        onUploadFailure,
        onDocumentSelection,
        onDocumentRemoval,
        onUploadEvent,
        submitUploads,
        uploadControls,
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
