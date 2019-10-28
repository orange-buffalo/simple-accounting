<template>
  <el-button
    type="text"
    @click="startDownload"
  >
    {{ document.name }}
    <template v-if="sizeKnown">
      ({{ sizeLabel }})
    </template>
  </el-button>
</template>

<script>
import { mapState } from 'vuex';
import FileSaver from 'file-saver';
import api from '@/services/api';

export default {
  name: 'DocumentLink',

  props: {
    document: Object,
  },

  computed: {
    ...mapState({
      workspaceId: state => state.workspaces.currentWorkspace.id,
    }),

    // todo #76: extend entity and localize
    sizeLabel() {
      return `<${this.document.sizeInBytes}>`;
    },

    sizeKnown() {
      return this.document.sizeInBytes;
    },
  },

  methods: {
    async startDownload() {
      const documentResponse = await api.get(`/workspaces/${this.workspaceId}/documents/${this.document.id}/content`, {
        responseType: 'blob',
        timeout: 30000,
      });
      FileSaver.saveAs(documentResponse.data, this.document.name);
    },
  },
};
</script>
