<template>
  <span>
    <span
      v-if="creatingDownloadLink"
      class="sa-download-link__loader"
    >
      {{ $t('saDocumentDownloadLink.creatingLinkMessage') }}
    </span>
    <ElButton
      v-else
      type="text"
      @click="startDownload"
    >
      {{ $t('saDocumentDownloadLink.label') }}
    </ElButton>
  </span>
</template>

<script lang="ts">
  import { ref, defineComponent } from '@vue/composition-api';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import { apiClient } from '@/services/api';

  function useDocumentsApi(documentId: number) {
    const creatingDownloadLink = ref(false);
    const { currentWorkspaceId } = useCurrentWorkspace();

    async function getDownloadToken() {
      creatingDownloadLink.value = true;
      try {
        const linkResponse = await apiClient.getDownloadToken({
          workspaceId: currentWorkspaceId,
          documentId,
        });
        return linkResponse.data.token;
      } finally {
        // let the document storage some time to prepare the data
        setTimeout(() => {
          creatingDownloadLink.value = false;
        }, 5000);
      }
    }

    return {
      getDownloadToken,
      creatingDownloadLink,
    };
  }

  function downloadFile(downloadToken: string, documentId: number, fileName: string) {
    const a = document.createElement('a');
    a.style.display = 'none';
    document.body.appendChild(a);
    a.href = `/api/downloads?token=${downloadToken}`;
    a.setAttribute('download', fileName);
    a.click();
    document.body.removeChild(a);
  }

  export default defineComponent({
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

    setup(props) {
      const {
        getDownloadToken,
        creatingDownloadLink,
      } = useDocumentsApi(props.documentId);

      async function startDownload() {
        const downloadToken = await getDownloadToken();
        downloadFile(downloadToken, props.documentId, props.documentName);
      }

      return {
        creatingDownloadLink,
        startDownload,
      };
    },
  });
</script>

<style lang="scss">
  .sa-download-link {
    &__loader {
      font-size: 80%;
    }
  }
</style>
