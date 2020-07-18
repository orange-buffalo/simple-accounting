import { onGetToWorkspacePath, responseDelay } from '@/stories/utils/stories-api-mocks';
import SaDocumentDownloadLink from '@/components/documents/SaDocumentDownloadLink';

export default {
  title: 'Components|SaDocumentDownloadLink',
};

export const Basic = () => ({
  components: { SaDocumentDownloadLink },
  template: '<SaDocumentDownloadLink :document-id="42" document-name="Service Agreement.pdf"/>',
  beforeCreate() {
    onGetToWorkspacePath('documents/:id/download-token')
      .intercept(async (req, res) => {
        await responseDelay(1000);
        res.json({ token: 'aHfndkFesdjDBs' });
      });
  },
});
