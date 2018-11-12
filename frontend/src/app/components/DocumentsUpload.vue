<template>
  <div>
    <!-- todo validation within el form -->

    <document-upload v-for="(upload, index) in uploads"
                     ref="uploads"
                     :key="upload"
                     @on-clear="onUploadClear(index)"
                     @on-select="onSelect(upload)"
                     />
  </div>
</template>

<script>
  import {mapState, mapGetters} from 'vuex'
  import DocumentUpload from './DocumentUpload'

  export default {
    name: 'DocumentsUpload',

    props: {

    },

    components: {
      DocumentUpload
    },

    data: function () {
      return {
        uploads: []
      }
    },

    created: function () {
      this.addNewUpload()
    },

    methods: {
      onUploadClear: function (uploadIndex) {
        if (this.uploads.length > 1) {
          this.uploads.splice(uploadIndex, 1)
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
          selected: false
        })
      }
    },

    computed: {

    }
  }
</script>

<style lang="scss">

</style>