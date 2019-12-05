<template>
  <div class="sa-documents-list">
    <SaDocument
      v-for="document in documents"
      :key="document.id"
      :document-name="document.name"
      :document-id="document.id"
      :document-size-in-bytes="document.sizeInBytes"
      class="sa-documents-list__document"
    />
  </div>
</template>

<script>
  import { api } from '@/services/api';
  import withWorkspaces from '@/components/mixins/with-workspaces';
  import SaDocument from '@/components/documents/SaDocument';

  export default {
    name: 'SaDocumentsList',

    components: { SaDocument },

    mixins: [withWorkspaces],

    props: {
      documentsIds: {
        type: Array,
        required: true,
      },
    },

    data() {
      return {
        documents: [],
      };
    },

    watch: {
      documentsIds() {
        this.loadDocuments();
      },
    },

    async created() {
      this.loadDocuments();
    },

    methods: {
      async loadDocuments() {
        if (this.documentsIds.length) {
          this.documents = await api.pageRequest(`/workspaces/${this.currentWorkspace.id}/documents`)
            .eager()
            .eqFilter('id', this.documentsIds)
            .getPageData();
        }
      },
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
