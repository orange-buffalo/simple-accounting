<template>
  <div>
    <SaDocumentsUpload
      ref="documentsUpload"
      :documents-ids="documentsIds"
      :loading-on-create="loadingOnCreate"
      style="width: 400px"
      @uploads-completed="onComplete"
      @uploads-failed="onFailure"
    />
    <div
      v-if="submittable"
      style="text-align: center"
    >
      <ElButton @click="startUpload">
        Start Upload
      </ElButton>
    </div>
  </div>
</template>

<script>
  import { ref } from '@vue/composition-api';
  // eslint-disable-next-line import/no-extraneous-dependencies
  import { action } from '@storybook/addon-actions';
  import SaDocumentsUpload from '@/components/documents/SaDocumentsUpload';

  export default {
    components: {
      SaDocumentsUpload,
    },

    props: {
      documentsIds: {
        type: Array,
        required: true,
      },
      loadingOnCreate: {
        type: Boolean,
        default: false,
      },
      submittable: {
        type: Boolean,
        default: true,
      },
    },

    setup() {
      const documentsUpload = ref(null);

      return {
        startUpload() {
          action('uploads-started')();
          documentsUpload.value.submitUploads();
        },
        onComplete(documents) {
          action('uploads-completed')(documents);
        },
        onFailure() {
          action('uploads-failed')();
        },
        documentsUpload,
      };
    },
  };
</script>
