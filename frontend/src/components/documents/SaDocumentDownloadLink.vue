<template>
  <ElTooltip
    :content="disabledTooltip"
    :disabled="!disabledTooltip"
    placement="bottom"
  >
    <span
      v-bind="$attrs"
      class="sa-document-download-link"
    >
      <span
        v-if="creatingDownloadLink"
        class="sa-download-link__loader"
      >
        {{ $t.saDocumentDownloadLink.creatingLinkMessage() }}
      </span>
      <ElButton
        v-else
        link
        :disabled="disabled"
        @click="startDownload"
      >
        {{ $t.saDocumentDownloadLink.label() }}
      </ElButton>
    </span>
  </ElTooltip>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import { $t } from '@/services/i18n';
  import { graphql } from '@/services/api/gql';
  import { useMutation } from '@/services/api/use-gql-api';

  const createDocumentDownloadUrl = useMutation(graphql(/* GraphQL */ `
    mutation createDocumentDownloadUrl($workspaceId: Long!, $documentId: Long!) {
      createDocumentDownloadUrl(workspaceId: $workspaceId, documentId: $documentId) {
        url
      }
    }
  `), 'createDocumentDownloadUrl');

  defineOptions({
    inheritAttrs: false,
  });

  function useDownloadUrl(documentId: number) {
    const creatingDownloadLink = ref(false);
    const { currentWorkspaceId } = useCurrentWorkspace();

    async function getDownloadUrl() {
      creatingDownloadLink.value = true;
      const response = await createDocumentDownloadUrl({
        workspaceId: currentWorkspaceId,
        documentId,
      });
      // let it hang for some time to look nicer
      setTimeout(() => {
        creatingDownloadLink.value = false;
      }, 3000);
      return response.url;
    }

    return {
      getDownloadUrl,
      creatingDownloadLink,
    };
  }

  function downloadFile(downloadUrl: string, fileName: string) {
    const a = document.createElement('a');
    a.style.display = 'none';
    document.body.appendChild(a);
    a.href = downloadUrl;
    a.setAttribute('download', fileName);
    a.click();
    document.body.removeChild(a);
  }

  const props = withDefaults(defineProps<{
    documentName: string,
    documentId: number,
    disabled?: boolean,
    disabledTooltip?: string,
  }>(), {
    disabled: false,
    disabledTooltip: undefined,
  });

  const {
    getDownloadUrl,
    creatingDownloadLink,
  } = useDownloadUrl(props.documentId);

  async function startDownload() {
    if (props.disabled) return;
    const downloadUrl = await getDownloadUrl();
    downloadFile(downloadUrl, props.documentName);
  }
</script>

<style lang="scss">
  .sa-download-link {
    &__loader {
      font-size: 80%;
    }
  }
</style>
