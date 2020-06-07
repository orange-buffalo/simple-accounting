import SaDocumentsUpload from '@/components/documents/SaDocumentsUpload';

export default {
  title: 'Components/SaDocumentsUpload',
};

export const NoExistingDocuments = () => ({
  components: { SaDocumentsUpload },
  template: '<SaDocumentsUpload :documents-ids="[]"/>',
});
