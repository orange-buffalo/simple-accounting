<template>
  <div>
    <el-form-item v-for="(upload, index) in uploadsInfo.uploads"
                  :prop="`${formProperty}.uploads.${index}`"
                  :key="upload.id"
                  :rules="validationRules">
      <document-upload
          ref="documentUploads"
          @upload-complete="onUploadComplete"
          @upload-error="onUploadError"
          v-model="uploadsInfo.uploads[index]"
      />
    </el-form-item>
  </div>
</template>

<script>
  import DocumentUpload from './DocumentUpload'
  import {UploadsInfo} from './uploads-info'

  export default {
    name: 'DocumentsUpload',

    props: {
      formProperty: String,
      value: UploadsInfo
    },

    components: {
      DocumentUpload
    },

    data: function () {
      return {
        uploadsInfo: this.value,
        validationRules: [
          {validator: this.validateUpload, trigger: 'change'}
        ]
      }
    },

    created: function () {
      this.addNewUpload()
    },

    methods: {
      addNewUpload: function () {
        this.uploadsInfo.add()
      },

      validateUpload: function (rule, value, callback) {
        value.validate(callback)
      },

      onUploadComplete: function () {
        this.onUploadDone()
      },

      onUploadError: function () {
        this.onUploadDone()
      },

      onUploadDone: function () {
        this.uploadsInfo.executeIfUploaded(() => {
          this.submitUploadPromise.resolve()
          this.submitUploadPromise = null
        }, () => {
          this.submitUploadPromise.reject()
          this.submitUploadPromise = false
        })
      },

      submitUploads: function () {
        return new Promise((resolve, reject) => {
          this.submitUploadPromise = {
            resolve: resolve,
            reject: reject
          }
          this.$refs.documentUploads.forEach(upload => upload.submitUpload())
          this.onUploadDone()
        })
      }
    },

    computed: {},

    watch: {
      uploadsInfo: {
        handler: function (val) {
          this.uploadsInfo.ensureCompleteness()
          this.$emit('input', val)
        },
        deep: true
      }
    }
  }
</script>

<style lang="scss">

</style>