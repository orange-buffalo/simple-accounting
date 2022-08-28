// noinspection JSUnusedGlobalSymbols

import { storybookData } from '@/__storybook__/storybook-data';
import DocumentsList from '@/components/documents/DocumentsList.vue';
import { defineStory } from '@/__storybook__/sa-storybook';
import {
  fetchMock, neverEndingGetRequest, onGetToDefaultWorkspacePath, pageResponse,
} from '@/__storybook__/api-mocks';
import { waitForText } from '@/__storybook__/screenshots';

function mockFileDownload() {
  fetchMock.get(/\/api\/workspaces\/42\/documents\/.*\/download-token/, {
    token: 'xxx',
  });
}

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
  title: 'Components/DocumentsList',
};

export const NoDocuments = defineStory(() => ({
  components: { DocumentsList },
  template: '<DocumentsList :documents-ids="[]" style="width: 400px"/>',
  beforeCreate() {
    mockSuccessStorageStatus();
  },
}));

export const WithDocuments = defineStory(() => ({
  components: { DocumentsList },
  setup: () => ({
    documentsIds: [
      storybookData.documents.lunaParkDeliveryAgreement.id,
      storybookData.documents.cheesePizzaAndALargeSodaReceipt.id,
      storybookData.documents.coffeeReceipt.id,
    ],
  }),
  template: '<DocumentsList :documents-ids="documentsIds"  style="width: 400px" />',
  beforeCreate() {
    mockSuccessStorageStatus();
    onGetToDefaultWorkspacePath(
      '/documents',
      pageResponse(
        storybookData.documents.lunaParkDeliveryAgreement,
        storybookData.documents.cheesePizzaAndALargeSodaReceipt,
        storybookData.documents.coffeeReceipt,
      ),
    );
    mockFileDownload();
  },
}), {
  screenshotPreparation: waitForText(storybookData.documents.coffeeReceipt.name),
  useRealTime: true,
});

export const WithLoadingDocuments = defineStory(() => ({
  components: { DocumentsList },
  template: '<DocumentsList :documents-ids="[77, 78]" style="width: 400px"/>',
  beforeCreate() {
    mockSuccessStorageStatus();
    onGetToDefaultWorkspacePath('/documents', {}, neverEndingGetRequest);
  },
}));

// screenshot is skipped for this case
export const WithDeferredDocuments = defineStory(() => ({
  components: { DocumentsList },
  setup: () => ({
    documentsIds: [
      storybookData.documents.lunaParkDeliveryAgreement.id,
      storybookData.documents.coffeeReceipt.id,
    ],
  }),
  template: '<DocumentsList :documents-ids="documentsIds" style="width: 400px"/>',
  beforeCreate() {
    mockSuccessStorageStatus();
    onGetToDefaultWorkspacePath(
      '/documents',
      pageResponse(
        storybookData.documents.lunaParkDeliveryAgreement,
        storybookData.documents.coffeeReceipt,
      ),
      { delay: 1000 },
    );
    mockFileDownload();
  },
}));

export const LoadingStorageStatus = defineStory(() => ({
  components: { DocumentsList },
  template: '<DocumentsList :documents-ids="[]" style="width: 400px"/>',
  beforeCreate() {
    mockLoadingStorageStatus();
  },
}));

export const FailedStorageStatus = defineStory(() => ({
  components: { DocumentsList },
  template: '<DocumentsList :documents-ids="[42]" style="width: 400px"/>',
  beforeCreate() {
    mockFailedStorageStatus();
  },
}), {
  screenshotPreparation: waitForText('Documents storage is not active'),
});
