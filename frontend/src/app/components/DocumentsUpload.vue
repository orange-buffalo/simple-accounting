<template>
  <div>
    <!-- todo validation within el form -->

    <el-form-item v-for="(upload, index) in uploads"
                  :prop="`${formProperty}.${index}`"
                  :key="index"
                  :rules="validationRules">
      <document-upload
          ref="uploads"
          @on-notes="onNotes(index, $event)"
          @on-clear="onUploadClear(index)"
          @on-select="onSelect(upload)"
      />
    </el-form-item>
  </div>
</template>

<script>
  import {mapState, mapGetters} from 'vuex'
  import DocumentUpload from './DocumentUpload'

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
      updateModel: function () {
        this.$emit('input', this.uploads)
      },

      onNotes: function (index, notes) {
        this.uploads[index].notes = notes
        this.updateModel()
      },

      onUploadClear: function (uploadIndex) {
        if (this.uploads.length > 1) {
          this.uploads.splice(uploadIndex, 1)
          this.updateModel()
        }
      },

      onSelect: function (upload) {
        upload.selected = true
        this.addNewUpload()
      },

      upload: function () {
        this.$refs.uploads.forEach(upload => upload.submit())
      },

      addNewUpload() {
        this.uploads.push({
          document: null,
          selected: false,
          notes: null
        })
        this.updateModel()
      },

      validateUpload: function (rule, value, callback) {
        if (value.notes && value.notes.length > 1024) {
          callback(new Error("Too long"))
        }
        else if (value.notes && !value.selected) {
          callback(new Error("Please select a file"))
        }
        else {
          callback()
        }
      }
    },

    computed: {}
  }
</script>

<style lang="scss">

</style>