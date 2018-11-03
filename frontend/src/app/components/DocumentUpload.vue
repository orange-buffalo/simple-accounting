<template>
  <div>
    <el-upload
        drag
        :show-file-list="false"
        :data="uploadRequest"
        :action="`/api/v1/user/workspaces/${workspaceId}/documents`"
        :on-success="onSuccess"
        :on-error="onError"
        :headers="headers">
      <i class="el-icon-upload"></i>
      <div class="el-upload__text">
        <div>Drop file here or <em>click to upload</em></div>
        <div class="el-upload__tip">jpg/png files with a size less than 500kb</div>
      </div>
    </el-upload>
    <el-input placeholder="Additional notes..." v-model="uploadRequest.notes"></el-input>
  </div>
</template>

<script>
  import api from '@/services/api'
  import {mapState, mapGetters} from 'vuex'

  export default {
    name: 'DocumentUpload',

    props: {
      // apiPath: {
      //   type: String,
      //   required: true
      // },


    },

    data: function () {
      // return {
      //   totalElements: 0,
      //   data: []
      // }
      return {
        uploadRequest: {
          notes: ""
        }
      }
    },

    created: function () {
      // this.currentPage = 1
      // this.pageSize = 10
      // this.reloadData()
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
      }
    }
  }
</script>