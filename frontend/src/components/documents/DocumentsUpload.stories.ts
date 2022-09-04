// noinspection JSUnusedGlobalSymbols

import { defineComponent, ref } from 'vue';
import { action } from '@storybook/addon-actions';
import { defineStory } from '@/__storybook__/sa-storybook';
import {
  defaultWorkspacePath, fetchMock, neverEndingGetRequest, pageResponse, pathOnlyMatcher,
} from '@/__storybook__/api-mocks';
import DocumentsUpload from '@/components/documents/DocumentsUpload.vue';
import { storybookData } from '@/__storybook__/storybook-data';
import { waitForElementToBeVisible, waitForText } from '@/__storybook__/screenshots';

function mockFailedStorageStatus() {
  fetchMock.get('api/profile/documents-storage', { active: false });
}

function mockSuccessStorageStatus() {
  fetchMock.get('api/profile/documents-storage', { active: true });
}

function mockLoadingStorageStatus() {
  fetchMock.get('api/profile/documents-storage', {}, neverEndingGetRequest);
}

export default {
  title: 'Components/DocumentsUpload',
  parameters: {
    useRealTime: true,
  },
};

const DocumentsUploadStories = defineComponent({
  components: {
    DocumentsUpload,
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
    const documentsUpload = ref<DocumentsUpload | null>(null);

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
    <DocumentsUpload
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
  beforeCreate() {
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
  beforeCreate() {
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
  beforeCreate() {
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
  beforeCreate() {
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
  beforeCreate() {
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
  beforeCreate() {
    mockSuccessStorageStatus();
  },
}), {
  screenshotPreparation: waitForText('Drop file here or click to upload'),
});

// this story is excluded from screenshot testing
export const AllUploadsFailing = defineStory(() => ({
  components: { DocumentsUploadStories },
  template: '<DocumentsUploadStories :documents-ids="[]" />',
  beforeCreate() {
    mockSuccessStorageStatus();
    fetchMock.post(defaultWorkspacePath('/documents'), {
      status: 404,
    });
  },
}));

export const LoadingStorageStatus = defineStory(() => ({
  components: { DocumentsUploadStories },
  template: '<DocumentsUploadStories :documents-ids="[]" :submittable="false"/>',
  beforeCreate() {
    mockLoadingStorageStatus();
  },
}));

export const FailedStorageStatus = defineStory(() => ({
  components: { DocumentsUploadStories },
  template: '<DocumentsUploadStories :documents-ids="[]" :submittable="false"/>',
  beforeCreate() {
    mockFailedStorageStatus();
  },
}), {
  screenshotPreparation: waitForText('Documents storage is not active'),
});
