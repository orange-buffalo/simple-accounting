// noinspection JSUnusedGlobalSymbols

import type { DocumentDto } from 'src/services/api';
import DocumentsList from '@/components/documents/DocumentsList.vue';
import { defineStory } from '@/__storybook__/sa-storybook';
import {
  defaultWorkspacePath, fetchMock, neverEndingGetRequest, pageResponse,
} from '@/__storybook__/api-mocks';
import { disableCssAnimations, disableDocumentLoaderAnimation, waitForText } from '@/__storybook__/screenshots';

let currentDocumentId = 100500;

function nextDocumentId() {
  currentDocumentId += 1;
  return currentDocumentId;
}

function document(fileName: string, sizeInBytes: number): DocumentDto {
  return {
    id: nextDocumentId(),
    version: 0,
    name: fileName,
    timeUploaded: new Date(),
    sizeInBytes,
  };
}

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
  template: '<DocumentsList :documents-ids="[77, 78, 79]"  style="width: 400px" />',
  beforeCreate() {
    mockSuccessStorageStatus();
    fetchMock.get(
      `path:${defaultWorkspacePath('/documents')}`,
      pageResponse(
        document('Service Agreement.pdf', 64422),
        document('Invoice #22.doc', 8222),
        document('Payment for Services.xlsx', 984843),
      ),
    );
    mockFileDownload();
  },
}), {
  screenshotPreparation: waitForText('Payment for Services.xlsx'),
});

export const WithLoadingDocuments = defineStory(() => ({
  components: { DocumentsList },
  template: '<DocumentsList :documents-ids="[77, 78]" style="width: 400px"/>',
  beforeCreate() {
    mockSuccessStorageStatus();
    fetchMock.get(`path:${defaultWorkspacePath('/documents')}`, {}, neverEndingGetRequest);
  },
}), {
  screenshotPreparation: disableDocumentLoaderAnimation(),
});

// screenshot is skipped for this case
export const WithDeferredDocuments = defineStory(() => ({
  components: { DocumentsList },
  template: '<DocumentsList :documents-ids="[77, 78]" style="width: 400px"/>',
  beforeCreate() {
    mockSuccessStorageStatus();
    fetchMock.get(
      `path:${defaultWorkspacePath('/documents')}`,
      pageResponse(
        document('Service Agreement.pdf', 64422),
        document('Invoice #22.pdf', 8222),
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
}), {
  screenshotPreparation: disableCssAnimations('.sa-documents-list__loading-placeholder'),
});

export const FailedStorageStatus = defineStory(() => ({
  components: { DocumentsList },
  template: '<DocumentsList :documents-ids="[]" style="width: 400px"/>',
  beforeCreate() {
    mockFailedStorageStatus();
  },
}), {
  screenshotPreparation: waitForText('Documents storage is not active'),
});
