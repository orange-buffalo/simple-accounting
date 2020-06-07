import SaDocumentsUpload from '@/components/documents/SaDocumentsUpload';
import mainConfig from '@/setup/setup-app';

const { app } = mainConfig;
app.store.commit('workspaces/setCurrentWorkspace', { id: 42 });

export default {
  title: 'Components/SaDocumentsUpload',
};

export const NoExistingDocuments = () => ({
  components: { SaDocumentsUpload },
  template: '<SaDocumentsUpload :documents-ids="[]"/>',
});
