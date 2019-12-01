<template>
  <div class="sa-documents_upload">
    <SaDocumentUpload
      v-for="documentAggregate in documentsAggregates"
      :key="documentAggregate.key"
      ref="uploadControls"
      :document-id="documentAggregate.document.id"
      :document-name="documentAggregate.document.name"
      :document-size-in-bytes="documentAggregate.document.sizeInBytes"
      class="sa-documents_upload__document"
      @upload-completed="onUploadComplete(documentAggregate.key, $event)"
      @upload-failed="onUploadFailure(documentAggregate.key)"
      @document-selected="onDocumentSelection(documentAggregate.key)"
      @document-removed="onDocumentRemoval(documentAggregate.key)"
    />
  </div>
</template>

<script>
  import SaDocumentUpload from '@/components/documents/SaDocumentUpload';
  import { api } from '@/services/api';
  import withWorkspaces from '@/components/mixins/with-workspaces';

  const DOCUMENT_AGGREGATE_STATE = {
    EMPTY: 'empty',
    PENDING: 'pending',
    UPLOAD_FAILED: 'upload-failed',
    UPLOAD_COMPLETED: 'upload-completed',
  };

  export default {
    name: 'SaDocumentsUpload',

    components: {
      SaDocumentUpload,
    },

    mixins: [withWorkspaces],

    props: {
      documentsIds: {
        type: Array,
        required: true,
      },
    },

    data() {
      return {
        documentsAggregates: [],
      };
    },

    watch: {
      async documentsIds() {
        if (!this.documentsIds.length) {
          this.documentsAggregates = [];
        } else {
          const documents = await api.pageRequest(`/workspaces/${this.currentWorkspace.id}/documents`)
            .eager()
            .eqFilter('id', this.documentsIds)
            .getPageData();

          this.documentsAggregates = documents.map(it => ({
            document: it,
            key: it.id.toString(),
            state: DOCUMENT_AGGREGATE_STATE.UPLOAD_COMPLETED,
          }));
        }
        this.addNewUploadControl();
      },
    },

    created() {
      this.addNewUploadControl();
    },

    methods: {
      addNewUploadControl() {
        this.documentsAggregates.push({
          document: {},
          key: new Date().getMilliseconds()
            .toString(),
          state: DOCUMENT_AGGREGATE_STATE.EMPTY,
        });
      },

      getDocumentAggregateByKey(key) {
        return this.documentsAggregates.find(it => it.key === key);
      },

      onUploadComplete(documentAggregateKey, uploadedDocument) {
        const documentAggregate = this.getDocumentAggregateByKey(documentAggregateKey);
        documentAggregate.state = DOCUMENT_AGGREGATE_STATE.UPLOAD_COMPLETED;
        documentAggregate.document = uploadedDocument;
        this.onUploadEvent();
      },

      onUploadFailure(documentAggregateKey) {
        const documentAggregate = this.getDocumentAggregateByKey(documentAggregateKey);
        documentAggregate.state = DOCUMENT_AGGREGATE_STATE.UPLOAD_FAILED;
        this.onUploadEvent();
      },

      onDocumentSelection(documentAggregateKey) {
        const documentAggregate = this.getDocumentAggregateByKey(documentAggregateKey);
        documentAggregate.state = DOCUMENT_AGGREGATE_STATE.PENDING;
        this.addNewUploadControl();
      },

      onDocumentRemoval(documentAggregateKey) {
        this.documentsAggregates = this.documentsAggregates.filter(it => it.key !== documentAggregateKey);
      },

      onUploadEvent() {
        const pendingUploads = this.documentsAggregates
          .filter(it => it.state === DOCUMENT_AGGREGATE_STATE.PENDING);
        if (pendingUploads.length) {
          return;
        }

        const failedUploads = this.documentsAggregates
          .filter(it => it.state === DOCUMENT_AGGREGATE_STATE.UPLOAD_FAILED);
        if (failedUploads.length) {
          this.$emit('uploads-failed');
          return;
        }

        const documentsIds = this.documentsAggregates
          .filter(it => it.state === DOCUMENT_AGGREGATE_STATE.UPLOAD_COMPLETED)
          .map(it => it.document.id);
        this.$emit('uploads-completed', documentsIds);
      },

      submitUploads() {
        this.$refs.uploadControls.forEach(upload => upload.submitUpload());
        this.onUploadEvent();
      },
    },
  };
</script>

<style lang="scss">
  .sa-documents_upload {
    &__document {
      margin-bottom: 15px;
      width: 100%;
    }
  }
</style>
