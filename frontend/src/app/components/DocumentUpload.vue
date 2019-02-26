<template>
  <div class="doc-upload">
    <template v-if="isDropPanelEnabled">
      <div ref="dropPanel"

           class="sa-document-upload_file-selector">
        <span>Drop file here or click to upload</span>
        <span v-if="error"
              class="sa-document-upload_file-selector-error">Files up to 5MB are allowed</span>
      </div>

      <el-input placeholder="Additional notes..."
                v-model="upload.notes"
                :clearable="true"/>
    </template>

    <template v-if="!isDropPanelEnabled">
      <div class="sa-document-upload_summary">
        <div class="sa-document-upload_summary_icon">
          <svgicon :name="documentTypeIcon"></svgicon>
        </div>
        <div class="sa-document-upload_summary_file">
          <div class="sa-document-upload_summary_header">
            <span :title="upload.name">{{upload.name}}</span>
            <svgicon name="delete"
                     @click="onRemove"
                     v-if="!uploading"></svgicon>
          </div>
          <div class="sa-document-upload_summary_status">
            <svgicon :name="documentStatusIcon"></svgicon>
            <span>{{documentStatus}}</span>
            <!--todo pretty print size-->
            <span>({{upload.size}})</span>
          </div>
          <div v-if="upload.hasNotes"
               class="sa-document-upload_summary_notes">
            {{upload.notes}}
          </div>
          <div>
            <el-progress :percentage="uploadingProgress"
                         v-if="uploading"></el-progress>
          </div>
          <div v-if="error"
               class="sa-document-upload_summary_error">
            Upload failed. Please try again
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script>
  import {mapState} from 'vuex'
  import emitter from 'element-ui/src/mixins/emitter'
  import {UploadInfo} from './uploads-info'
  import Dropzone from 'dropzone'
  import {isNil} from 'lodash'
  import '@/components/icons/done'
  import '@/components/icons/delete'
  import '@/components/icons/upload'
  import '@/components/icons/file'
  import '@/components/icons/pdf'
  import '@/components/icons/doc'
  import '@/components/icons/jpg'
  import '@/components/icons/zip'

  Dropzone.autoDiscover = false;

  export default {
    name: 'DocumentUpload',

    props: {
      value: UploadInfo
    },

    mixins: [emitter],

    data: function () {
      return {
        uploadRequest: {
          notes: ''
        },
        upload: this.value,
        uploading: false,
        uploadingProgress: 0,
        error: false
      }
    },

    mounted: function () {
      if (!this.dropzone && this.$refs.dropPanel) {
        this.dropzone = new Dropzone(this.$refs.dropPanel, {
          url: `/api/v1/user/workspaces/${this.workspaceId}/documents`,
          paramName: "file",
          createImageThumbnails: false,
          autoProcessQueue: false,
          previewTemplate: '<span>',
          accept: (file, done) => {
            this.error = false
            if (file.size > 5 * 1024 * 1024) {
              this.error = true
              done()
              this.dropzone.removeAllFiles()
            } else {
              this.upload.selectFile(file)
              this.dispatch('ElFormItem', 'el.form.change');
              done()
            }
          }
        });

        this.dropzone.on('uploadprogress', (file, progress) => {
          this.uploadingProgress = progress
        })

        this.dropzone.on('error', (file, error) => {
          //todo special processing for storage service config error
          //todo handle 401 by acquiring new token and restarting upload
          this.uploading = false
          this.error = true
          this.upload.uploadError = error
          this.dropzone.files[0].status = 'queued'
          this.$emit('upload-error', error)
        })

        this.dropzone.on('success', (file, response) => {
          this.uploading = false
          this.upload.document = response
          this.$emit('upload-complete', this.upload)
        })

        this.dropzone.on("processing", () => {
          if (!isNil(this.uploadRequest.notes)) {
            this.dropzone.options.params = {notes: this.uploadRequest.notes}
          }
          this.dropzone.options.headers = this.headers
        });
      }
    },

    destroyed: function () {
      if (this.dropzone) {
        this.dropzone.destroy()
      }
    },

    methods: {
      onRemove: function () {
        if (this.dropzone) {
          this.dropzone.removeAllFiles()
        }
        this.upload.clear()
        this.dispatch('ElFormItem', 'el.form.change');
      },

      submitUpload: function () {
        if (!this.upload.isEmpty() && !this.upload.isDocumentUploaded()) {
          this.upload.uploadError = null
          this.uploading = true
          this.error = false
          this.uploadingProgress = 0
          this.dropzone.processQueue()
        }
      }
    },

    computed: {
      ...mapState({
        workspaceId: state => state.workspaces.currentWorkspace.id,
        bearerToken: state => state.api.jwtToken
      }),

      headers: function () {
        return {
          'Authorization': `Bearer ${this.bearerToken}`
        }
      },

      isDropPanelEnabled: function () {
        return !this.upload.isFileSelected() && !this.upload.isDocumentUploaded()
      },

      documentStatusIcon: function () {
        return this.upload.isDocumentUploaded() ? 'done' : 'upload'
      },

      documentStatus: function () {
        //todo i18n
        return this.upload.isDocumentUploaded() ? 'Uploaded' : 'New file to be uploaded'
      },

      documentTypeIcon: function () {
        let fileName = this.upload.name.toLowerCase()
        if (fileName.endsWith('.pdf')) {
          return 'pdf'
        } else if (fileName.endsWith('.jpg') || fileName.endsWith('.jpeg')) {
          return 'jpg'
        } else if (fileName.endsWith('.zip') || fileName.endsWith('.gz') || fileName.endsWith('.rar')) {
          return 'zip'
        } else if (fileName.endsWith('.doc') || fileName.endsWith('.docx')) {
          return 'doc'
        } else {
          return 'file'
        }
      }
    },

    watch: {
      upload: {
        handler: function (val) {
          this.$emit('input', val)
          this.uploadRequest.notes = val.notes
        },
        deep: true
      },

      value: {
        handler: function (val) {
          this.upload = val
        },
        deep: true
      }
    }
  }
</script>

<style lang="scss">
  @import "@/app/styles/vars.scss";

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