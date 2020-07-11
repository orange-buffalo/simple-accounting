import {
  apiPage, onGet, onGetToWorkspacePath, responseDelay,
} from '@/stories/utils/stories-api-mocks';
import SaDocumentsList from '@/components/documents/SaDocumentsList';

let nextDocumentId = 100500;

function document(fileName, sizeInBytes) {
  return {
    // eslint-disable-next-line no-plusplus
    id: nextDocumentId++,
    version: 0,
    name: fileName,
    timeUploaded: Date(),
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

export default {
  title: 'Components|SaDocumentsList',
};

export const NoDocuments = () => ({
  components: { SaDocumentsList },
  template: '<SaDocumentsList :documents-ids="[]" style="width: 400px"/>',
  beforeCreate() {
    mockSuccessStorageStatus();
  },
});

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

export const WithDefferedDocuments = () => ({
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

export const LoadingStorageStatus = () => ({
  components: { SaDocumentsList },
  template: '<SaDocumentsList :documents-ids="[]" style="width: 400px"/>',
  beforeCreate() {
    mockLoadingStorageStatus();
  },
});

export const FailedStorageStatus = () => ({
  components: { SaDocumentsList },
  template: '<SaDocumentsList :documents-ids="[]" style="width: 400px"/>',
  beforeCreate() {
    mockFailedStorageStatus();
  },
});
