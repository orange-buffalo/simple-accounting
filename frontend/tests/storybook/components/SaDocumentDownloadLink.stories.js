import SaDocumentDownloadLink from '@/components/documents/SaDocumentDownloadLink';
import { onGetToWorkspacePath, responseDelay } from '../utils/stories-api-mocks';

export default {
  title: 'Components/SaDocumentDownloadLink',
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
