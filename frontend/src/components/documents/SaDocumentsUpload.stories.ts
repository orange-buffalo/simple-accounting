// noinspection JSUnusedGlobalSymbols

import { ElButton } from 'element-plus';
import { defineComponent, ref } from 'vue';
import { action } from '@storybook/addon-actions';
import { defineStory } from '@/__storybook__/sa-storybook';
import {
  defaultWorkspacePath,
  fetchMock, mockFailedStorageStatus,
  mockLoadingStorageStatus,
  mockSuccessStorageStatus,
  neverEndingGetRequest,
  pageResponse,
  pathOnlyMatcher,
} from '@/__storybook__/api-mocks';
import SaDocumentsUpload from '@/components/documents/SaDocumentsUpload.vue';
import { storybookData } from '@/__storybook__/storybook-data';
import { waitForElementToBeVisible, waitForText } from '@/__storybook__/screenshots';

export default {
  title: 'Components/Basic/Documents/SaDocumentsUpload',
  parameters: {
    useRealTime: true,
  },
};

const DocumentsUploadStories = defineComponent({
  components: {
    SaDocumentsUpload,
    ElButton,
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
    const documentsUpload = ref<typeof SaDocumentsUpload | null>(null);

    return {
      startUpload() {
        action('uploads-started')();
        documentsUpload.value?.submitUploads();
      },
      onComplete(documents: number[]) {
        action('uploads-completed')(documents);
      },
      onFailure() {
        action('uploads-failed')();
      },
      documentsUpload,
    };
  },
  template: `
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
      <ElButton @click="startUpload" type="primary">
        Start Upload
      </ElButton>
    </div>
    </div>
  `,
});

export const LoadingOnCreateIsSet = defineStory(() => ({
  components: { DocumentsUploadStories },
  template: '<DocumentsUploadStories :documents-ids="[]" loading-on-create :submittable="false"/>',
  setup() {
    mockLoadingStorageStatus();
  },
}));

export const WithDocuments = defineStory(() => ({
  components: { DocumentsUploadStories },
  data: () => ({
    documentIds: [
      storybookData.documents.cheesePizzaAndALargeSodaReceipt.id,
      storybookData.documents.coffeeReceipt.id,
      storybookData.documents.lunaParkDeliveryAgreement.id,
    ],
  }),
  template: '<DocumentsUploadStories :documents-ids="documentIds" />',
  setup() {
    mockSuccessStorageStatus();
    fetchMock.get(pathOnlyMatcher(defaultWorkspacePath('/documents')), pageResponse(
      storybookData.documents.cheesePizzaAndALargeSodaReceipt,
      storybookData.documents.coffeeReceipt,
      storybookData.documents.lunaParkDeliveryAgreement,
    ));
  },
}), {
  screenshotPreparation: waitForText(storybookData.documents.coffeeReceipt.name),
});

export const WithLoadingDocuments = defineStory(() => ({
  components: { DocumentsUploadStories },
  template: '<DocumentsUploadStories :documents-ids="[42, 43]" :submittable="false"/>',
  setup() {
    mockSuccessStorageStatus();
    fetchMock.get(pathOnlyMatcher(defaultWorkspacePath('/documents')), {}, neverEndingGetRequest);
  },
}), {
  screenshotPreparation: waitForElementToBeVisible('.sa-document'),
});

// this story is excluded from screenshot testing
export const WithDeferredDocuments = defineStory(() => ({
  components: { DocumentsUploadStories },
  data() {
    return {
      documents: [],
    };
  },
  template: '<DocumentsUploadStories :documents-ids="documents" loading-on-create :submittable="false"/>',
  setup() {
    mockSuccessStorageStatus();
    fetchMock.get(pathOnlyMatcher(defaultWorkspacePath('/documents')), pageResponse(
      storybookData.documents.lunaParkDeliveryAgreement,
      storybookData.documents.coffeeReceipt,
    ));
  },
  mounted() {
    setTimeout(() => {
      this.documents = [77, 78];
    }, 1000);
  },
}));

// this story is excluded from screenshot testing
export const InitialLoadingWithNoDocuments = defineStory(() => ({
  components: { DocumentsUploadStories },
  data() {
    return { documents: [] };
  },
  template: '<DocumentsUploadStories :documents-ids="documents" loading-on-create :submittable="false"/>',
  setup() {
    mockSuccessStorageStatus();
  },
  mounted() {
    setTimeout(() => {
      this.documents = [];
    }, 1000);
  },
}));

export const EmptyDocuments = defineStory(() => ({
  components: { DocumentsUploadStories },
  template: '<DocumentsUploadStories :documents-ids="[]" :submittable="false" />',
  setup() {
    mockSuccessStorageStatus();
  },
}), {
  screenshotPreparation: waitForText('Drop file here or click to upload'),
});

// this story is excluded from screenshot testing
export const AllUploadsFailing = defineStory(() => ({
  components: { DocumentsUploadStories },
  template: '<DocumentsUploadStories :documents-ids="[]" />',
  setup() {
    mockSuccessStorageStatus();
    fetchMock.post(defaultWorkspacePath('/documents'), {
      status: 404,
    });
  },
}));

export const LoadingStorageStatus = defineStory(() => ({
  components: { DocumentsUploadStories },
  template: '<DocumentsUploadStories :documents-ids="[]" :submittable="false"/>',
  setup() {
    mockLoadingStorageStatus();
  },
}));

export const FailedStorageStatus = defineStory(() => ({
  components: { DocumentsUploadStories },
  template: '<DocumentsUploadStories :documents-ids="[]" :submittable="false"/>',
  setup() {
    mockFailedStorageStatus();
  },
}), {
  screenshotPreparation: waitForText('Documents storage is not active'),
});
