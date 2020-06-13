import {
  apiPage, onGetToWorkspacePath, onPostToWorkspacePath, responseDelay,
} from '@/stories/utils/stories-api-mocks';
import SaDocumentsUploadStories from '@/stories/components/SaDocumentsUploadStories';

let nextDocumentId = 100500;
let shouldFailInToggleTest = true;

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

export default {
  title: 'Components/SaDocumentsUpload',
};

export const LoadingOnCreateIsSet = () => ({
  components: { SaDocumentsUploadStories },
  template: '<SaDocumentsUploadStories :documents-ids="[]" loading-on-create :submittable="false"/>',
});
LoadingOnCreateIsSet.story = {
  name: 'loading-on-create is set',
};

export const WithDocuments = () => ({
  components: { SaDocumentsUploadStories },
  template: '<SaDocumentsUploadStories :documents-ids="[77, 78, 79]" />',
  beforeCreate() {
    onGetToWorkspacePath('documents')
      .successJson(apiPage([
        document('Service Agreement.pdf', 64422),
        document('Invoice #22', 8222),
        document('Payment for Services', 984843),
      ]));
  },
});

export const WithDefferedDocuments = () => ({
  components: { SaDocumentsUploadStories },
  data() {
    return {
      documents: [],
    };
  },
  template: '<SaDocumentsUploadStories :documents-ids="documents" loading-on-create :submittable="false"/>',
  beforeCreate() {
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

export const NoExistingDocuments = () => ({
  components: { SaDocumentsUploadStories },
  template: '<SaDocumentsUploadStories :documents-ids="[]" :submittable="false" />',
});

export const AllUploadsFailing = () => ({
  components: { SaDocumentsUploadStories },
  template: '<SaDocumentsUploadStories :documents-ids="[]" />',
  beforeCreate() {
    onPostToWorkspacePath('documents')
      .reply(404);
  },
});

export const AllUploadsSucceeding = () => ({
  components: { SaDocumentsUploadStories },
  template: '<SaDocumentsUploadStories :documents-ids="[]" />',
  beforeCreate() {
    onPostToWorkspacePath('documents')
      .successJson(document('Service Agreement.pdf', 77000));
  },
});

export const EverySecondFileSucceeding = () => ({
  components: { SaDocumentsUploadStories },
  template: '<SaDocumentsUploadStories :documents-ids="[]" />',
  beforeCreate() {
    onPostToWorkspacePath('documents')
      .intercept(async (req, res) => {
        await responseDelay(1000);

        if (shouldFailInToggleTest) {
          res.status(500);
        } else {
          res.json({
            // eslint-disable-next-line no-plusplus
            id: nextDocumentId++,
            version: 0,
            name: 'kubuntu-19.04-desktop-amd64.iso',
            timeUploaded: Date(),
            sizeInBytes: 77000,
          });
        }
        shouldFailInToggleTest = !shouldFailInToggleTest;
      });
  },
});
