// noinspection JSUnusedGlobalSymbols

import { storybookData } from '@/__storybook__/storybook-data';
import SaDocumentsList from '@/components/documents/SaDocumentsList.vue';
import { defineStory } from '@/__storybook__/sa-storybook';
import {
  fetchMock, mockFailedStorageStatus,
  mockLoadingStorageStatus,
  mockSuccessStorageStatus,
  neverEndingGetRequest,
  onGetToDefaultWorkspacePath,
  pageResponse,
} from '@/__storybook__/api-mocks';
import { waitForText } from '@/__storybook__/screenshots';

function mockFileDownload() {
  fetchMock.get(/\/api\/workspaces\/42\/documents\/.*\/download-token/, {
    token: 'xxx',
  });
}

export default {
  title: 'Components/Basic/Documents/SaDocumentsList',
};

export const NoDocuments = defineStory(() => ({
  components: { SaDocumentsList },
  template: '<SaDocumentsList :documents-ids="[]" style="width: 400px"/>',
  setup() {
    mockSuccessStorageStatus();
  },
}));

export const WithDocuments = defineStory(() => ({
  components: { SaDocumentsList },
  setup: () => {
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
    return ({
      documentsIds: [
        storybookData.documents.lunaParkDeliveryAgreement.id,
        storybookData.documents.cheesePizzaAndALargeSodaReceipt.id,
        storybookData.documents.coffeeReceipt.id,
      ],
    });
  },
  template: '<SaDocumentsList :documents-ids="documentsIds"  style="width: 400px" />',
}), {
  screenshotPreparation: waitForText(storybookData.documents.coffeeReceipt.name),
  useRealTime: true,
});

export const WithLoadingDocuments = defineStory(() => ({
  components: { SaDocumentsList },
  template: '<SaDocumentsList :documents-ids="[77, 78]" style="width: 400px"/>',
  setup() {
    mockSuccessStorageStatus();
    onGetToDefaultWorkspacePath('/documents', {}, neverEndingGetRequest);
  },
}));

// screenshot is skipped for this case
export const WithDeferredDocuments = defineStory(() => ({
  components: { SaDocumentsList },
  setup: () => {
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
    return ({
      documentsIds: [
        storybookData.documents.lunaParkDeliveryAgreement.id,
        storybookData.documents.coffeeReceipt.id,
      ],
    });
  },
  template: '<SaDocumentsList :documents-ids="documentsIds" style="width: 400px"/>',
}));

export const LoadingStorageStatus = defineStory(() => ({
  components: { SaDocumentsList },
  template: '<SaDocumentsList :documents-ids="[]" style="width: 400px"/>',
  setup() {
    mockLoadingStorageStatus();
  },
}));

export const FailedStorageStatus = defineStory(() => ({
  components: { SaDocumentsList },
  template: '<SaDocumentsList :documents-ids="[42]" style="width: 400px"/>',
  setup() {
    mockFailedStorageStatus();
  },
}), {
  screenshotPreparation: waitForText('Documents storage is not active'),
});
