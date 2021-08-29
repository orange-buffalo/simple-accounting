<template>
  <div>
    <template v-if="fileSelectorAvailable">
      <div
        ref="dropPanel"
        class="sa-document-upload__file-selector"
      >
        <span>{{ $t('saDocumentUpload.fileSelector.message') }}</span>
        <span
          v-if="uploadingFailed"
          class="sa-document-upload__file-selector-error"
        > {{ $t('saDocumentUpload.fileSelector.hint', [maxFileSize]) }} </span>
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
            :class="{'sa-document-upload__status_error': status.failure}"
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

<script lang="ts">
  import {
    computed, defineComponent, onBeforeUnmount, onMounted, reactive, ref,
  } from '@vue/composition-api';
  import Dropzone, { DropzoneFile } from 'dropzone';
  import SaIcon from '@/components/SaIcon';
  import SaDocument from '@/components/documents/SaDocument';
  import { useAuth } from '@/services/api';
  import i18n from '@/services/i18n';
  import { useCurrentWorkspace } from '@/services/workspaces';

  Dropzone.autoDiscover = false;

  interface Document {
    id: number | null,
    name: string| null,
    sizeInBytes: number| null,
  }

  export default defineComponent({
    components: {
      SaDocument,
      SaIcon,
    },

    props: {
      documentId: {
        type: Number,
        default: null,
      },

      documentName: {
        type: String,
        default: null,
      },

      documentSizeInBytes: {
        type: Number,
        default: null,
      },
    },

    // todo: refactor to split into multiple functions
    setup(props, { emit }) {
      const document = reactive<Document>({
        id: props.documentId,
        name: props.documentName,
        sizeInBytes: props.documentSizeInBytes,
      });
      const uploading = ref(false);
      const uploadingProgress = ref(0);
      const uploadingFailed = ref(false);
      const selectedFile = ref<DropzoneFile | null>(null);

      const { getToken } = useAuth();

      const documentAvailable = computed(() => document.id != null);

      const fileSelectorAvailable = computed(() => !documentAvailable.value && selectedFile.value == null);

      const displayedSize = computed(() => (uploadingFailed.value ? null : document.sizeInBytes));

      const status = computed(() => {
        if (uploadingFailed.value) {
          return {
            icon: 'error',
            text: i18n.t('saDocumentUpload.uploadStatusMessage.error'),
            failure: true,
          };
        }

        if (uploading.value) {
          return {
            icon: 'upload',
            text: i18n.t('saDocumentUpload.uploadStatusMessage.uploading'),
          };
        }

        return {
          icon: 'upload',
          text: i18n.t('saDocumentUpload.uploadStatusMessage.scheduled'),
        };
      });

      const maxFileSize = 50 * 1024 * 1024;
      const dropPanel = ref<HTMLElement | null>(null);
      let dropzone : Dropzone | null;
      const { currentWorkspaceId } = useCurrentWorkspace();
      const headers = {
        Authorization: `Bearer ${getToken()}`,
      };
      onMounted(() => {
        if (!dropzone && dropPanel.value) {
          dropzone = new Dropzone(dropPanel.value, {
            url: `/api/workspaces/${currentWorkspaceId}/documents`,
            paramName: 'file',
            createImageThumbnails: false,
            autoProcessQueue: false,
            previewTemplate: '<span>',
            accept: (file, done) => {
              uploadingFailed.value = false;
              if (file.size > maxFileSize) {
                uploadingFailed.value = true;
                done();
                dropzone!.removeAllFiles();
              } else {
                selectedFile.value = file;
                document.id = null;
                document.name = file.name;
                document.sizeInBytes = file.size;
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
            // todo #77: handle 401 by acquiring new token and restarting upload
            uploading.value = false;
            uploadingFailed.value = true;
            dropzone!.files[0].status = 'queued';
            emit('upload-failed', error);
          });

          dropzone.on('success', (file, response) => {
            uploading.value = false;
            const {
              id,
              name,
              sizeInBytes,
            } = response as any; // todo type
            document.id = id;
            document.name = name;
            document.sizeInBytes = sizeInBytes;
            emit('upload-completed', document);
          });

          dropzone.on('processing', () => {
            dropzone!.options.headers = headers;
          });
        }
      });

      onBeforeUnmount(() => {
        if (dropzone) {
          dropzone.destroy();
          dropzone = null;
        }
      });

      const onRemove = () => {
        if (dropzone) {
          dropzone.removeAllFiles();
        }
        document.id = null;
        document.name = null;
        document.sizeInBytes = null;
        emit('document-removed');
      };

      const submitUpload = () => {
        if (selectedFile.value && !documentAvailable.value) {
          uploading.value = true;
          uploadingFailed.value = false;
          uploadingProgress.value = 0;
          dropzone!.processQueue();
        }
      };

      return {
        document,
        uploading,
        uploadingProgress,
        uploadingFailed,
        selectedFile,
        headers,
        maxFileSize,
        documentAvailable,
        fileSelectorAvailable,
        displayedSize,
        status,
        onRemove,
        submitUpload,
        dropPanel,
      };
    },
  });
</script>

<style lang="scss">
  @import "~@/styles/vars.scss";

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
