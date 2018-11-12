<template>
  <div>
    <el-upload
        :drag="!filePresent"
        :show-file-list="false"
        :data="uploadRequest"
        :action="`/api/v1/user/workspaces/${workspaceId}/documents`"
        :on-success="onSuccess"
        :on-error="onError"
        :on-progress="onProgress"
        :on-change="onChange"
        ref="upload"
        :disabled="filePresent"
        :auto-upload="false"
        :headers="headers">
      <div v-if="!filePresent">
        <i class="el-icon-upload"></i>
        <div class="el-upload__text">
          <div>Drop file here or <em>click to upload</em></div>
          <div class="el-upload__tip">jpg/png files with a size less than 500kb</div>
        </div>
      </div>

      <div v-if="filePresent" class="doc-upload-file-panel">
        <i class="el-icon-document"></i>
        <div class="el-upload__text">
          <!--todo display in localized kb/mb units-->
          <div>{{selectedFile.name}} ({{selectedFile.size}})</div>
          <div class="el-upload__tip">
            <el-button type="text" @click="onRemove">Remove</el-button>
          </div>
        </div>
      </div>
    </el-upload>
    <el-input placeholder="Additional notes..." v-model="uploadRequest.notes"></el-input>
  </div>
</template>

<script>
  import {mapState, mapGetters} from 'vuex'

  export default {
    name: 'DocumentUpload',

    props: {},

    data: function () {
      return {
        uploadRequest: {
          notes: ""
        },
        selectedFile: null
      }
    },

    methods: {
      onSuccess: function (response) {
        this.$emit('upload-complete', response)
      },

      onError: function (error) {
        console.error(error)
        this.$message({
          showClose: true,
          message: 'Upload failed',
          type: 'error'
        });
      },

      onProgress: function (event) {
        console.log(event)
      },

      onChange: function (file) {
        console.log(file)
        if (!this.filePresent) {
          this.selectedFile = file
        }
      },

      onRemove: function () {
        this.selectedFile = null
        if (!this.uploadRequest.notes) {
          this.$emit('on-clear')
        }
      },

      upload: function () {
        this.$refs.upload.submit()
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
        return this.selectedFile != null
      }
    }
  }
</script>

<style lang="scss">
  @import "~element-ui/packages/theme-chalk/src/upload";

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