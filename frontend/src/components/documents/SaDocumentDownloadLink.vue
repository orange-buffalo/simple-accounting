<template>
  <span>
    <span
      v-if="creatingDownloadLink"
      class="sa-download-link__loader"
    >
      {{ $t.saDocumentDownloadLink.creatingLinkMessage() }}
    </span>
    <ElButton
      v-else
      link
      @click="startDownload"
    >
      {{ $t.saDocumentDownloadLink.label() }}
    </ElButton>
  </span>
</template>

<script lang="ts" setup>
import { ref } from 'vue';
import { documentsApi } from '@/services/api';
import { $t } from '@/services/i18n';
import { useCurrentWorkspace } from '@/services/workspaces';

function useDocumentsApi(documentId: number) {
  const creatingDownloadLink = ref(false);
  const { currentWorkspaceId } = useCurrentWorkspace();

  async function getDownloadToken() {
    creatingDownloadLink.value = true;
    const tokenResponse = await documentsApi.getDownloadToken({
      workspaceId: currentWorkspaceId,
      documentId,
    });
    // let it hang for some time to look nicer
    setTimeout(() => {
      creatingDownloadLink.value = false;
    }, 3000);
    return tokenResponse.token;
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

const props = defineProps<{
  documentName: string;
  documentId: number;
}>();

const { getDownloadToken, creatingDownloadLink } = useDocumentsApi(props.documentId);

async function startDownload() {
  const downloadToken = await getDownloadToken();
  downloadFile(downloadToken, props.documentId, props.documentName);
}
</script>

<style lang="scss">
  .sa-download-link {
    &__loader {
      font-size: 80%;
    }
  }
</style>
