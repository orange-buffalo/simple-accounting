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
        <div
          v-if="!documentAvailable"
          slot="extras"
          class="sa-document-upload__status"
          :class="{'sa-document-upload__status_error': status.failure}"
        >
          <SaIcon
            :icon="status.icon"
            class="sa-document-upload__status-icon"
          />
          <span>{{ status.text }}</span>
        </div>
      </SaDocument>
    </template>
  </div>
</template>

<script>
  import Dropzone from 'dropzone';
  import { api } from '@/services/api-legacy';
  import SaIcon from '@/components/SaIcon';
  import SaDocument from '@/components/documents/SaDocument';
  import withWorkspaces from '@/components/mixins/with-workspaces';

  Dropzone.autoDiscover = false;

  export default {
    name: 'SaDocumentUpload',

    components: {
      SaDocument,
      SaIcon,
    },

    mixins: [withWorkspaces],

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

    data() {
      return {
        document: {
          id: this.documentId,
          name: this.documentName,
          sizeInBytes: this.documentSizeInBytes,
        },
        uploading: false,
        uploadingProgress: 0,
        uploadingFailed: false,
        selectedFile: null,
        headers: {
          Authorization: `Bearer ${api.getToken()}`,
        },
        maxFileSize: 50 * 1024 * 1024,
      };
    },

    computed: {
      documentAvailable() {
        return this.document.id != null;
      },

      fileSelectorAvailable() {
        return !this.documentAvailable && this.selectedFile == null;
      },

      displayedSize() {
        return this.uploadingFailed ? null : this.document.sizeInBytes;
      },

      status() {
        if (this.uploadingFailed) {
          return {
            icon: 'error',
            text: this.$t('saDocumentUpload.uploadStatusMessage.error'),
            failure: true,
          };
        }

        if (this.uploading) {
          return {
            icon: 'upload',
            text: this.$t('saDocumentUpload.uploadStatusMessage.uploading'),
          };
        }

        return {
          icon: 'upload',
          text: this.$t('saDocumentUpload.uploadStatusMessage.scheduled'),
        };
      },
    },

    mounted() {
      if (!this.dropzone && this.$refs.dropPanel) {
        this.dropzone = new Dropzone(this.$refs.dropPanel, {
          url: `/api/workspaces/${this.currentWorkspace.id}/documents`,
          paramName: 'file',
          createImageThumbnails: false,
          autoProcessQueue: false,
          previewTemplate: '<span>',
          accept: (file, done) => {
            this.uploadingFailed = false;
            if (file.size > this.maxFileSize) {
              this.uploadingFailed = true;
              done();
              this.dropzone.removeAllFiles();
            } else {
              this.selectedFile = file;
              this.document = {
                id: null,
                name: file.name,
                sizeInBytes: file.size,
              };
              this.$emit('document-selected');
              done();
            }
          },
        });

        this.dropzone.on('uploadprogress', (file, progress) => {
          this.uploadingProgress = progress;
        });

        this.dropzone.on('error', (file, error) => {
          // todo #72: special processing for storage service config error
          // todo #77: handle 401 by acquiring new token and restarting upload
          this.uploading = false;
          this.uploadingFailed = true;
          this.dropzone.files[0].status = 'queued';
          this.$emit('upload-failed', error);
        });

        this.dropzone.on('success', (file, response) => {
          this.uploading = false;
          const { id, name, sizeInBytes } = response;
          this.document = {
            id,
            name,
            sizeInBytes,
          };
          this.$emit('upload-completed', this.document);
        });

        this.dropzone.on('processing', () => {
          this.dropzone.options.headers = this.headers;
        });
      }
    },

    destroyed() {
      if (this.dropzone) {
        this.dropzone.destroy();
        this.dropzone = null;
      }
    },

    methods: {
      onRemove() {
        if (this.dropzone) {
          this.dropzone.removeAllFiles();
        }
        this.document = {
          id: null,
          name: null,
          sizeInBytes: null,
        };
        this.$emit('document-removed');
      },

      submitUpload() {
        if (this.selectedFile && !this.documentAvailable) {
          this.uploading = true;
          this.uploadingFailed = false;
          this.uploadingProgress = 0;
          this.dropzone.processQueue();
        }
      },
    },
  };
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
