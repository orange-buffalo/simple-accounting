<template>
  <div>
    <!-- todo validation within el form -->

    <el-form-item v-for="(upload, index) in uploads"
                  :prop="`${formProperty}.${index}`"
                  :key="upload.id"
                  :rules="validationRules">
      <document-upload
          ref="uploads"
          v-model="uploads[index]"
      />
    </el-form-item>
  </div>
</template>

<script>
  import DocumentUpload from './DocumentUpload'
  import UploadInfo from './upload-info'

  export default {
    name: 'DocumentsUpload',

    props: {
      formProperty: String,
      value: Array
    },

    components: {
      DocumentUpload
    },

    data: function () {
      return {
        uploads: this.value,
        validationRules: [
          {validator: this.validateUpload, trigger: 'change'}
        ]
      }
    },

    created: function () {
      this.addNewUpload()
    },

    methods: {
      upload: function () {
        this.$refs.uploads.forEach(upload => upload.submit())
      },

      addNewUpload() {
        this.uploads.push(new UploadInfo())
      },

      validateUpload: function (rule, value, callback) {
        if (value.notes && value.notes.length > 1024) {
          callback(new Error("Too long"))
        }
        else if (value.notes && !value.isFileSelected()) {
          callback(new Error("Please select a file"))
        }
        else {
          callback()
        }
      }
    },

    computed: {},

    watch: {
      uploads: {
        handler: function (val) {
          if (val[val.length - 1].isFileSelected()) {
            this.uploads.push(new UploadInfo())
          }

          if (val.length > 1) {
            for (let i = 0; i < val.length - 1; i++) {
              if (val[i].isEmpty()) {
                this.uploads.splice(i, 1)
              }
            }
          }
          this.$emit('input', val)
        },
        deep: true
      }
    }
  }
</script>

<style lang="scss">

</style>