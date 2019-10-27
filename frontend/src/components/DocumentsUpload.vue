<template>
  <div>
    <el-form-item
      v-for="(upload, index) in uploadsInfo.uploads"
      :key="upload.id"
      :prop="`${formProperty}.uploads.${index}`"
      :rules="validationRules"
    >
      <document-upload
        ref="documentUploads"
        v-model="uploadsInfo.uploads[index]"
        @upload-complete="onUploadComplete"
        @upload-error="onUploadError"
      />
    </el-form-item>
  </div>
</template>

<script>
import DocumentUpload from './DocumentUpload';
import { UploadsInfo } from './uploads-info';

export default {
  name: 'DocumentsUpload',

  components: {
    DocumentUpload,
  },

  props: {
    formProperty: String,
    value: UploadsInfo,
  },

  data() {
    return {
      uploadsInfo: this.value,
      validationRules: [
        { validator: this.validateUpload, trigger: 'change' },
      ],
    };
  },

  computed: {},

  watch: {
    uploadsInfo: {
      handler(val) {
        this.uploadsInfo.ensureCompleteness();
        this.$emit('input', val);
      },
      deep: true,
    },
  },

  created() {
    this.addNewUpload();
  },

  methods: {
    addNewUpload() {
      this.uploadsInfo.add();
    },

    validateUpload(rule, value, callback) {
      value.validate(callback);
    },

    onUploadComplete() {
      this.onUploadDone();
    },

    onUploadError() {
      this.onUploadDone();
    },

    onUploadDone() {
      this.uploadsInfo.executeIfUploaded(() => {
        this.submitUploadPromise.resolve();
        this.submitUploadPromise = null;
      }, () => {
        this.submitUploadPromise.reject();
        this.submitUploadPromise = false;
      });
    },

    submitUploads() {
      return new Promise((resolve, reject) => {
        this.submitUploadPromise = {
          resolve,
          reject,
        };
        this.$refs.documentUploads.forEach(upload => upload.submitUpload());
        this.onUploadDone();
      });
    },
  },
};
</script>

<style lang="scss">

</style>
