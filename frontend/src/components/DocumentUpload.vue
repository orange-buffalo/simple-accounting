<template>
  <div class="doc-upload">
    <template v-if="isDropPanelEnabled">
      <div
        ref="dropPanel"

        class="sa-document-upload_file-selector"
      >
        <span>Drop file here or click to upload</span>
        <span
          v-if="error"
          class="sa-document-upload_file-selector-error"
        >Files up to 5MB are allowed</span>
      </div>

      <ElInput
        v-model="upload.notes"
        placeholder="Additional notes..."
        clearable
      />
    </template>

    <template v-if="!isDropPanelEnabled">
      <div class="sa-document-upload_summary">
        <div class="sa-document-upload_summary_icon">
          <Svgicon :name="documentTypeIcon" />
        </div>
        <div class="sa-document-upload_summary_file">
          <div class="sa-document-upload_summary_header">
            <span :title="upload.name">{{ upload.name }}</span>
            <Svgicon
              v-if="!uploading"
              name="delete"
              @click="onRemove"
            />
          </div>
          <div class="sa-document-upload_summary_status">
            <Svgicon :name="documentStatusIcon" />
            <span>{{ documentStatus }}</span>
            <!--todo #76: pretty print size-->
            <span>({{ upload.size }})</span>
          </div>
          <div
            v-if="upload.hasNotes"
            class="sa-document-upload_summary_notes"
          >
            {{ upload.notes }}
          </div>
          <div>
            <ElProgress
              v-if="uploading"
              :percentage="uploadingProgress"
            />
          </div>
          <div
            v-if="error"
            class="sa-document-upload_summary_error"
          >
            Upload failed. Please try again
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script>
  import { mapState } from 'vuex';
  import emitter from 'element-ui/src/mixins/emitter';
  import Dropzone from 'dropzone';
  import { isNil } from 'lodash';
  import { UploadInfo } from './uploads-info';
  import '@/components/icons/done';
  import '@/components/icons/delete';
  import '@/components/icons/upload';
  import '@/components/icons/file';
  import '@/components/icons/pdf';
  import '@/components/icons/doc';
  import '@/components/icons/jpg';
  import '@/components/icons/zip';

  Dropzone.autoDiscover = false;

  export default {
    name: 'DocumentUpload',

    mixins: [emitter],

    props: {
      value: UploadInfo,
    },

    data() {
      return {
        uploadRequest: {
          notes: '',
        },
        upload: this.value,
        uploading: false,
        uploadingProgress: 0,
        error: false,
      };
    },

    mounted() {
      if (!this.dropzone && this.$refs.dropPanel) {
        this.dropzone = new Dropzone(this.$refs.dropPanel, {
          url: `/api/workspaces/${this.workspaceId}/documents`,
          paramName: 'file',
          createImageThumbnails: false,
          autoProcessQueue: false,
          previewTemplate: '<span>',
          accept: (file, done) => {
            this.error = false;
            if (file.size > 5 * 1024 * 1024) {
              this.error = true;
              done();
              this.dropzone.removeAllFiles();
            } else {
              this.upload.selectFile(file);
              this.dispatch('ElFormItem', 'el.form.change');
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
          this.error = true;
          this.upload.uploadError = error;
          this.dropzone.files[0].status = 'queued';
          this.$emit('upload-error', error);
        });

        this.dropzone.on('success', (file, response) => {
          this.uploading = false;
          this.upload.document = response;
          this.$emit('upload-complete', this.upload);
        });

        this.dropzone.on('processing', () => {
          if (!isNil(this.uploadRequest.notes)) {
            this.dropzone.options.params = { notes: this.uploadRequest.notes };
          }
          this.dropzone.options.headers = this.headers;
        });
      }
    },

    destroyed() {
      if (this.dropzone) {
        this.dropzone.destroy();
      }
    },

    methods: {
      onRemove() {
        if (this.dropzone) {
          this.dropzone.removeAllFiles();
        }
        this.upload.clear();
        this.dispatch('ElFormItem', 'el.form.change');
      },

      submitUpload() {
        if (!this.upload.isEmpty() && !this.upload.isDocumentUploaded()) {
          this.upload.uploadError = null;
          this.uploading = true;
          this.error = false;
          this.uploadingProgress = 0;
          this.dropzone.processQueue();
        }
      },
    },

    computed: {
      ...mapState({
        workspaceId: state => state.workspaces.currentWorkspace.id,
        bearerToken: state => state.api.jwtToken,
      }),

      headers() {
        return {
          Authorization: `Bearer ${this.bearerToken}`,
        };
      },

      isDropPanelEnabled() {
        return !this.upload.isFileSelected() && !this.upload.isDocumentUploaded();
      },

      documentStatusIcon() {
        return this.upload.isDocumentUploaded() ? 'done' : 'upload';
      },

      documentStatus() {
        // todo #6: i18n
        return this.upload.isDocumentUploaded() ? 'Uploaded' : 'New file to be uploaded';
      },

      documentTypeIcon() {
        const fileName = this.upload.name.toLowerCase();
        if (fileName.endsWith('.pdf')) {
          return 'pdf';
        } if (fileName.endsWith('.jpg') || fileName.endsWith('.jpeg')) {
          return 'jpg';
        } if (fileName.endsWith('.zip') || fileName.endsWith('.gz') || fileName.endsWith('.rar')) {
          return 'zip';
        } if (fileName.endsWith('.doc') || fileName.endsWith('.docx')) {
          return 'doc';
        }
        return 'file';
      },
    },

    watch: {
      upload: {
        handler(val) {
          this.$emit('input', val);
          this.uploadRequest.notes = val.notes;
        },
        deep: true,
      },

      value: {
        handler(val) {
          this.upload = val;
        },
        deep: true,
      },
    },
  };
</script>

<style lang="scss">
  @import "@/styles/vars.scss";
  @import "@/styles/mixins.scss";

  .sa-document-upload_file-selector {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    border: dashed 1px $components-border-color;
    border-radius: 2px;
    min-height: 80px;
    margin-bottom: 10px;
    transition: all 250ms ease-out;
    color: $primary-color-lighter-iii;
    @include input-width;

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

  .sa-document-upload_file-selector-error {
    color: $danger-color;
    font-size: 90%;
  }

  .sa-document-upload_summary {
    display: flex;

    .el-progress__text {
      color: $secondary-text-color;
      font-size: 80% !important;
    }
  }

  .sa-document-upload_summary_icon {
    margin-right: 5px;

    .svg-icon {
      width: 40px;
      height: 40px;
    }
  }

  .sa-document-upload_summary_notes {
    width: 100%;
    line-height: 1em;
    color: $secondary-text-color;
    font-size: 90%;
  }

  .sa-document-upload_summary_file {
    width: 100%;
    line-height: 1em;
  }

  .sa-document-upload_summary_error {
    color: $danger-color;
    font-size: 90%;
  }

  .sa-document-upload_summary_status {
    color: $secondary-text-color;
    font-size: 90%;
    display: flex;

    .svg-icon {
      margin-right: 3px;
    }

    span {
      display: inline-block;
      margin-right: 3px;
    }
  }

  .sa-document-upload_summary_header {
    display: flex;
    justify-content: space-between;
    line-height: 1em;
    margin-bottom: 5px;

    span {
      display: inline-block;
      max-width: calc(100% - 30px);
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .svg-icon {
      cursor: pointer;
      color: $components-color;
      width: 10px;
      height: 10px;
    }
  }
</style>
