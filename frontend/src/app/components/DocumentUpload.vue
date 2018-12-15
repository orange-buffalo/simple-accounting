<template>
  <div class="doc-upload">
    <el-upload
        :drag="!filePresent && !documentUploaded"
        :show-file-list="false"
        :data="uploadRequest"
        :action="`/user/workspaces/${workspaceId}/documents`"
        :on-success="onSuccess"
        :on-error="onError"
        :on-progress="onProgress"
        :on-change="onChange"
        ref="elUpload"
        :disabled="filePresent || documentUploaded"
        :auto-upload="false"
        :headers="headers">
      <div v-if="!filePresent  && !documentUploaded">
        <i class="el-icon-upload"></i>
        <div class="el-upload__text">
          <div>Drop file here or <em>click to upload</em></div>
          <div class="el-upload__tip">jpg/png files with a size less than 500kb</div>
        </div>
      </div>

      <div v-if="filePresent || documentUploaded" class="doc-upload-file-panel">
        <i class="el-icon-document"></i>
        <div class="el-upload__text">
          <!--todo display in localized kb/mb units-->
          <div>{{upload.name}} ({{upload.size}})</div>
          <div class="el-upload__tip">
            <el-button type="text" @click="onRemove">Remove</el-button>
          </div>
        </div>
      </div>
    </el-upload>
    <el-input placeholder="Additional notes..."
              v-model="upload.notes"
              :clearable="true"
              v-if="!documentUploaded"/>

    <span v-if="documentUploaded"
          class="doc-upload-notes">{{upload.notes}}</span>
  </div>
</template>

<script>
  import {mapState} from 'vuex'
  import emitter from 'element-ui/src/mixins/emitter'
  import {UploadInfo} from './uploads-info'

  export default {
    name: 'DocumentUpload',

    props: {
      value: UploadInfo
    },

    mixins: [emitter],

    data: function () {
      return {
        uploadRequest: {
          notes: ""
        },
        upload: this.value
      }
    },

    methods: {
      onSuccess: function (response) {
        console.log(response)
        this.upload.document = response
        this.$emit('upload-complete', this.upload)
      },

      onError: function (error) {
        console.log(error)
        //todo change component visual state to indicate upload error
        this.upload.uploadError = error
        this.$emit('upload-error', error)
      },

      onProgress: function (event) {
        // todo: add progress component to display upload status
        console.log(event)
      },

      onChange: function (file) {
        this.upload.selectFile(file)
        this.dispatch('ElFormItem', 'el.form.change');
      },

      onRemove: function () {
        // todo remove document if already was uploaded
        this.$refs.elUpload.clearFiles()
        this.upload.clear()
        this.dispatch('ElFormItem', 'el.form.change');
      },

      submitUpload: function () {
        if (!this.upload.isEmpty() && !this.upload.isDocumentUploaded()) {
          this.$refs.elUpload.submit()
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

      filePresent: function () {
        return this.upload.isFileSelected()
      },

      documentUploaded: function () {
        return this.upload.isDocumentUploaded()
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
  @import "~element-ui/packages/theme-chalk/src/upload";

  .doc-upload-notes {
    font-style: italic;
  }

  .el-form-item {
    &.is-error {
      .el-upload-dragger {
        border-color: red;
      }
    }
  }

  .doc-upload-file-panel {
    @extend .el-upload-dragger;
    border: solid 1px #dcdfe6;
    cursor: initial;

    &:hover {
      border: solid 1px #dcdfe6;
    }

    .el-icon-document {
      font-size: 67px;
      color: #c0c4cc;
      margin: 40px 0 16px;
      line-height: 50px;
    }

  }

  .el-upload__text {
    line-height: 30px;

    .el-upload__tip {
      margin-top: 0;
    }
  }

  .el-upload {
    width: 100%;

    .el-upload-dragger {
      width: 100%;
    }
  }
</style>