import {
  apiPage, onGet, onGetToWorkspacePath, onPostToWorkspacePath, responseDelay,
} from '../utils/stories-api-mocks';
import SaDocumentsUploadStories from '../components/SaDocumentsUploadStories';
import {
  NO_STORYSHOTS_STORY,
  pauseAndResetAnimation,
  pauseAndResetDocumentLoaderAnimation, setViewportHeight,
  storyshotsStory,
} from '../utils/stories-utils';

let nextDocumentId = 100500;
let shouldFailInToggleTest = true;

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

async function pauseAndResetLoadingIndicator(page) {
  await pauseAndResetAnimation(page, '.sa-documents-upload__loading-placeholder');
}

// noinspection JSUnusedGlobalSymbols
export default {
  title: 'Components|SaDocumentsUpload',
};

export const LoadingOnCreateIsSet = () => ({
  components: { SaDocumentsUploadStories },
  template: '<SaDocumentsUploadStories :documents-ids="[]" loading-on-create :submittable="false"/>',
  beforeCreate() {
    mockLoadingStorageStatus();
  },
});
LoadingOnCreateIsSet.story = {
  name: 'loading-on-create is set',
  ...storyshotsStory({
    async setup(page) {
      await pauseAndResetLoadingIndicator(page);
    },
  }),
};

export const WithDocuments = () => ({
  components: { SaDocumentsUploadStories },
  template: '<SaDocumentsUploadStories :documents-ids="[77, 78, 79]" />',
  beforeCreate() {
    mockSuccessStorageStatus();
    onGetToWorkspacePath('documents')
      .successJson(apiPage([
        document('Service Agreement.pdf', 64422),
        document('Invoice #22', 8222),
        document('Payment for Services', 984843),
      ]));
  },
});
WithDocuments.story = storyshotsStory({
  async setup(page) {
    await setViewportHeight(page, 400);
  },
});

export const WithLoadingDocuments = () => ({
  components: { SaDocumentsUploadStories },
  template: '<SaDocumentsUploadStories :documents-ids="[42, 43]" :submittable="false"/>',
  beforeCreate() {
    mockSuccessStorageStatus();
    onGetToWorkspacePath('documents')
      .neverEndingRequest();
  },
});
WithLoadingDocuments.story = storyshotsStory({
  // width is calculated as a fractional value, antialiasing causes flaky test
  matchOptions: {
    failureThreshold: 100,
    failureThresholdType: 'pixel',
  },
  async setup(page) {
    await pauseAndResetDocumentLoaderAnimation(page);
  },
});

export const WithDeferredDocuments = () => ({
  components: { SaDocumentsUploadStories },
  data() {
    return {
      documents: [],
    };
  },
  template: '<SaDocumentsUploadStories :documents-ids="documents" loading-on-create :submittable="false"/>',
  beforeCreate() {
    mockSuccessStorageStatus();
    onGetToWorkspacePath('documents')
      .intercept(async (req, res) => {
        await responseDelay(1000);
        res.json(apiPage([
          document('Service Agreement.pdf', 64422),
          document('Invoice #22', 8222),
        ]));
      });
  },
  mounted() {
    setTimeout(() => {
      this.documents = [77, 78];
    }, 1000);
  },
});
WithDeferredDocuments.story = NO_STORYSHOTS_STORY;

export const InitialLoadingWithNoDocuments = () => ({
  components: { SaDocumentsUploadStories },
  data() {
    return { documents: [] };
  },
  template: '<SaDocumentsUploadStories :documents-ids="documents" loading-on-create :submittable="false"/>',
  beforeCreate() {
    mockSuccessStorageStatus();
  },
  mounted() {
    const that = this;
    setTimeout(() => {
      that.documents = [];
    }, 1000);
  },
});
InitialLoadingWithNoDocuments.story = NO_STORYSHOTS_STORY;

// noinspection JSUnusedGlobalSymbols
export const NoExistingDocuments = () => ({
  components: { SaDocumentsUploadStories },
  template: '<SaDocumentsUploadStories :documents-ids="[]" :submittable="false" />',
  beforeCreate() {
    mockSuccessStorageStatus();
  },
});

export const AllUploadsFailing = () => ({
  components: { SaDocumentsUploadStories },
  template: '<SaDocumentsUploadStories :documents-ids="[]" />',
  beforeCreate() {
    mockSuccessStorageStatus();
    onPostToWorkspacePath('documents')
      .reply(404);
  },
});
AllUploadsFailing.story = NO_STORYSHOTS_STORY;

export const AllUploadsSucceeding = () => ({
  components: { SaDocumentsUploadStories },
  template: '<SaDocumentsUploadStories :documents-ids="[]" />',
  beforeCreate() {
    mockSuccessStorageStatus();
    onPostToWorkspacePath('documents')
      .successJson(() => document('Service Agreement.pdf', 77000));
  },
});
AllUploadsSucceeding.story = NO_STORYSHOTS_STORY;

export const EverySecondFileSucceeding = () => ({
  components: { SaDocumentsUploadStories },
  template: '<SaDocumentsUploadStories :documents-ids="[]" />',
  beforeCreate() {
    mockSuccessStorageStatus();
    onPostToWorkspacePath('documents')
      .intercept(async (req, res) => {
        await responseDelay(1000);

        if (shouldFailInToggleTest) {
          res.status(500);
        } else {
          res.json(document('Service Agreement.pdf', 77000));
        }
        shouldFailInToggleTest = !shouldFailInToggleTest;
      });
  },
});
EverySecondFileSucceeding.story = NO_STORYSHOTS_STORY;

export const LoadingStorageStatus = () => ({
  components: { SaDocumentsUploadStories },
  template: '<SaDocumentsUploadStories :documents-ids="[]" :submittable="false"/>',
  beforeCreate() {
    mockLoadingStorageStatus();
  },
});
LoadingStorageStatus.story = storyshotsStory({
  async setup(page) {
    await pauseAndResetLoadingIndicator(page);
  },
});

// noinspection JSUnusedGlobalSymbols
export const FailedStorageStatus = () => ({
  components: { SaDocumentsUploadStories },
  template: '<SaDocumentsUploadStories :documents-ids="[]" :submittable="false"/>',
  beforeCreate() {
    mockFailedStorageStatus();
  },
});
