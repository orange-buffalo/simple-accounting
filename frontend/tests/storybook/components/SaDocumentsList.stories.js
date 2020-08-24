import SaDocumentsList from '@/components/documents/SaDocumentsList';
import {
  apiPage, onGet, onGetToWorkspacePath, responseDelay,
} from '../utils/stories-api-mocks';
import { pauseAndResetAnimation, pauseAndResetDocumentLoaderAnimation } from '../utils/stories-utils';

let nextDocumentId = 100500;

function document(fileName, sizeInBytes) {
  return {
    // eslint-disable-next-line no-plusplus
    id: nextDocumentId++,
    version: 0,
    name: fileName,
    timeUploaded: new Date(),
    sizeInBytes,
  };
}

function mockFileDownload() {
  onGetToWorkspacePath('documents/:id/content')
    .intercept((req, res) => {
      res.type('application/octet-stream')
        .send('File content');
    });
}

function mockFailedStorageStatus() {
  onGet('api/profile/documents-storage')
    .successJson({ active: false });
}

function mockSuccessStorageStatus() {
  onGet('api/profile/documents-storage')
    .successJson({ active: true });
}

function mockLoadingStorageStatus() {
  onGet('api/profile/documents-storage')
    .neverEndingRequest();
}

// noinspection JSUnusedGlobalSymbols
export default {
  title: 'Components/SaDocumentsList',
};

// noinspection JSUnusedGlobalSymbols
export const NoDocuments = () => ({
  components: { SaDocumentsList },
  template: '<SaDocumentsList :documents-ids="[]" style="width: 400px"/>',
  beforeCreate() {
    mockSuccessStorageStatus();
  },
});

// noinspection JSUnusedGlobalSymbols
export const WithDocuments = () => ({
  components: { SaDocumentsList },
  template: '<SaDocumentsList :documents-ids="[77, 78, 79]"  style="width: 400px" />',
  beforeCreate() {
    mockSuccessStorageStatus();
    onGetToWorkspacePath('documents')
      .successJson(apiPage([
        document('Service Agreement.pdf', 64422),
        document('Invoice #22.doc', 8222),
        document('Payment for Services.xlsx', 984843),
      ]));
    mockFileDownload();
  },
});

export const WithLoadingDocuments = () => ({
  components: { SaDocumentsList },
  template: '<SaDocumentsList :documents-ids="[77, 78]" style="width: 400px"/>',
  beforeCreate() {
    mockSuccessStorageStatus();
    onGetToWorkspacePath('documents')
      .neverEndingRequest();
  },
});
WithLoadingDocuments.parameters = {
  storyshots: {
    // width is calculated as a fractional value, antialiasing causes flaky test
    matchOptions: {
      failureThreshold: 100,
      failureThresholdType: 'pixel',
    },
    async setup(page) {
      await pauseAndResetDocumentLoaderAnimation(page);
    },
  },
};

export const WithDeferredDocuments = () => ({
  components: { SaDocumentsList },
  template: '<SaDocumentsList :documents-ids="[77, 78]" style="width: 400px"/>',
  beforeCreate() {
    mockSuccessStorageStatus();
    onGetToWorkspacePath('documents')
      .intercept(async (req, res) => {
        await responseDelay(1000);
        res.json(apiPage([
          document('Service Agreement.pdf', 64422),
          document('Invoice #22.pdf', 8222),
        ]));
      });
    mockFileDownload();
  },
});
WithDeferredDocuments.parameters = {
  storyshots: false,
};

export const LoadingStorageStatus = () => ({
  components: { SaDocumentsList },
  template: '<SaDocumentsList :documents-ids="[]" style="width: 400px"/>',
  beforeCreate() {
    mockLoadingStorageStatus();
  },
});
LoadingStorageStatus.parameters = {
  storyshots: {
    async setup(page) {
      await pauseAndResetAnimation(page, '.sa-documents-list__loading-placeholder');
    },
  },
};

// noinspection JSUnusedGlobalSymbols
export const FailedStorageStatus = () => ({
  components: { SaDocumentsList },
  template: '<SaDocumentsList :documents-ids="[]" style="width: 400px"/>',
  beforeCreate() {
    mockFailedStorageStatus();
  },
});
