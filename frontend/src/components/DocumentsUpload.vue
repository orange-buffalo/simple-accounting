<template>
  <div>
    <ElFormItem
      v-for="(upload, index) in uploadsInfo.uploads"
      :key="upload.id"
      :prop="`${formProperty}.uploads.${index}`"
      :rules="validationRules"
    >
      <DocumentUpload
        ref="documentUploads"
        v-model="uploadsInfo.uploads[index]"
        @upload-complete="onUploadComplete"
        @upload-error="onUploadError"
      />
    </ElFormItem>
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
      formProperty: {
        type: String,
        required: true,
      },
      value: {
        type: UploadsInfo,
        required: true,
      },
    },

    data() {
      return {
        uploadsInfo: this.value,
        validationRules: [
          {
            validator: this.validateUpload,
            trigger: 'change',
          },
        ],
      };
    },

    watch: {
      value() {
        this.uploadsInfo = this.value;
        this.addNewUpload();
      },

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
