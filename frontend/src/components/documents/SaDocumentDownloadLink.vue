<template>
  <ElButton
    type="text"
    @click="startDownload"
  >
    Download
  </ElButton>
</template>

<script>
  import FileSaver from 'file-saver';
  import { api } from '@/services/api';
  import withWorkspaces from '@/components/mixins/with-workspaces';

  export default {
    name: 'SaDocumentDownloadLink',

    mixins: [withWorkspaces],

    props: {
      documentName: {
        type: String,
        required: true,
      },

      documentId: {
        type: Number,
        required: true,
      },
    },

    methods: {
      async startDownload() {
        const documentResponse = await api
          .get(`/workspaces/${this.currentWorkspace.id}/documents/${this.documentId}/content`, {
            responseType: 'blob',
            timeout: 30000,
          });
        FileSaver.saveAs(documentResponse.data, this.documentName);
      },
    },
  };
</script>
