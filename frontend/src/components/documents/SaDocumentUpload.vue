<template>
  <div>
    <template v-if="fileSelectorAvailable">
      <div
        ref="dropPanel"
        class="sa-document-upload__file-selector"
      >
        <span>{{ $t.saDocumentUpload.fileSelector.message() }}</span>
        <span
          v-if="uploadingFailed"
          class="sa-document-upload__file-selector-error"
        > {{ $t.saDocumentUpload.fileSelector.hint(maxFileSize) }} </span>
      </div>
    </template>

    <template v-else>
      <SaDocument
        :document-id="document.id"
        :document-name="document.name"
        :document-size-in-bytes="displayedSize"
        :in-progress="uploading"
        :progress="uploadingProgress"
        :removable="true"
        @removed="onRemove"
      >
        <template #extras>
          <div
            v-if="!documentAvailable"
            class="sa-document-upload__status"
            :class="{ 'sa-document-upload__status_error': status.failure }"
          >
            <SaIcon
              :icon="status.icon"
              class="sa-document-upload__status-icon"
            />
            <span>{{ status.text }}</span>
          </div>
        </template>
      </SaDocument>
    </template>
  </div>
</template>

<script lang="ts" setup>
  import {
    computed, onBeforeUnmount, onMounted, ref,
  } from 'vue';
  import type { DropzoneFile } from 'dropzone';
  import Dropzone from 'dropzone';
  import SaIcon from '@/components/SaIcon.vue';
  import SaDocument from '@/components/documents/SaDocument.vue';
  import { $t } from '@/services/i18n';
  import { graphql } from '@/services/api/gql';
  import { useMutation } from '@/services/api/use-gql-api';
  import { useCurrentWorkspace } from '@/services/workspaces';

  Dropzone.autoDiscover = false;

  export interface UploadedDocumentDto {
    id: number;
    name: string;
    sizeInBytes?: number;
  }

  const props = defineProps<{
    documentId?: number,
    documentName?: string,
    documentSizeInBytes?: number,
  }>();

  const emit = defineEmits<{(e: 'document-removed'): void,
                            (e: 'document-selected'): void,
                            (e: 'upload-failed', error: string | Error): void,
                            (e: 'upload-completed', document: UploadedDocumentDto): void,
  }>();

  const document = ref<Partial<UploadedDocumentDto>>({
    id: props.documentId,
    name: props.documentName,
    sizeInBytes: props.documentSizeInBytes,
  });
  const uploading = ref(false);
  const uploadingProgress = ref(0);
  const uploadingFailed = ref(false);
  const selectedFile = ref<DropzoneFile | undefined>(undefined);

  const documentAvailable = computed(() => document.value.id !== undefined);

  const fileSelectorAvailable = computed(() => !documentAvailable.value && selectedFile.value === undefined);

  const displayedSize = computed(() => (uploadingFailed.value ? undefined : document.value.sizeInBytes));

  const status = computed<{
    icon: string,
    text: string,
    failure?: boolean,
  }>(() => {
    if (uploadingFailed.value) {
      return {
        icon: 'error',
        text: $t.value.saDocumentUpload.uploadStatusMessage.error(),
        failure: true,
      };
    }

    if (uploading.value) {
      return {
        icon: 'upload',
        text: $t.value.saDocumentUpload.uploadStatusMessage.uploading(),
      };
    }

    return {
      icon: 'upload',
      text: $t.value.saDocumentUpload.uploadStatusMessage.scheduled(),
    };
  });

  const createDocumentUploadUrlMutation = graphql(`
    mutation createDocumentUploadUrl($workspaceId: Long!) {
      createDocumentUploadUrl(workspaceId: $workspaceId) {
        url
        filePartName
      }
    }
  `);

  const executeCreateUploadUrl = useMutation(
    createDocumentUploadUrlMutation,
    'createDocumentUploadUrl',
  );

  const { currentWorkspaceId } = useCurrentWorkspace();

  const maxFileSize = 50 * 1024 * 1024;
  const dropPanel = ref<HTMLElement | undefined>(undefined);
  let dropzone: Dropzone | undefined;
  const dropzoneRequired = () => {
    if (!dropzone) throw new Error('Not initialized yet');
    return dropzone;
  };
  onMounted(() => {
    if (!dropzone && dropPanel.value) {
      dropzone = new Dropzone(dropPanel.value as HTMLElement, {
        url: '/api/documents/upload/placeholder',
        paramName: 'file',
        createImageThumbnails: false,
        autoProcessQueue: false,
        previewTemplate: '<span>',
        accept: (file, done) => {
          uploadingFailed.value = false;
          if (file.size > maxFileSize) {
            uploadingFailed.value = true;
            done();
            dropzoneRequired().removeAllFiles();
          } else {
            selectedFile.value = file;
            document.value.id = undefined;
            document.value.name = file.name;
            document.value.sizeInBytes = file.size;
            emit('document-selected');
            done();
          }
        },
      });

      dropzone.on('uploadprogress', (file, progress) => {
        uploadingProgress.value = progress;
      });

      dropzone.on('error', (file, error) => {
        // todo #72: special processing for storage service config error
        uploading.value = false;
        uploadingFailed.value = true;
        dropzoneRequired().files[0].status = 'queued';
        emit('upload-failed', error);
      });

      dropzone.on('success', (file, response) => {
        uploading.value = false;
        document.value = response as UploadedDocumentDto;
        emit('upload-completed', document.value as UploadedDocumentDto);
      });
    }
  });

  onBeforeUnmount(() => {
    if (dropzone) {
      dropzone.destroy();
      dropzone = undefined;
    }
  });

  const onRemove = () => {
    if (dropzone) {
      dropzone.removeAllFiles();
    }
    document.value.id = undefined;
    document.value.name = undefined;
    document.value.sizeInBytes = undefined;
    emit('document-removed');
  };

  const submitUpload = async () => {
    if (selectedFile.value && !documentAvailable.value) {
      uploading.value = true;
      uploadingFailed.value = false;
      uploadingProgress.value = 0;
      try {
        const uploadUrlResponse = await executeCreateUploadUrl({ workspaceId: currentWorkspaceId });
        const dz = dropzoneRequired();
        dz.options.url = uploadUrlResponse.url;
        dz.options.paramName = uploadUrlResponse.filePartName;
        dz.processQueue();
      } catch (e) {
        uploading.value = false;
        uploadingFailed.value = true;
        emit('upload-failed', e instanceof Error ? e : new Error(String(e)));
      }
    }
  };

  defineExpose({
    submitUpload,
  });
</script>

<style lang="scss">
  @use "@/styles/vars.scss" as *;

  .sa-document-upload {
    &__file-selector {
      width: 100%;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      border: dashed 1px $components-border-color;
      border-radius: 2px;
      min-height: 80px;
      transition: all 250ms ease-out;
      color: $primary-color-lighter-iii;

      span {
        pointer-events: none;
        line-height: 1.5em;
      }

      &.dz-drag-hover, &:hover {
        color: $primary-color-lighter-ii;
        border-color: $primary-color-lighter-iii;
        background-color: $primary-grey;
      }

      .dz-complete {
        display: none;
      }
    }

    &__file-selector-error {
      color: $danger-color;
      font-size: 90%;
    }

    &__status {
      color: $secondary-text-color;
      display: flex;
      align-items: center;

      span {
        display: inline-block;
        margin-right: 3px;
      }

      &_error {
        color: $danger-color;
      }
    }

    &__status-icon {
      margin-right: 3px;
    }
  }
</style>
