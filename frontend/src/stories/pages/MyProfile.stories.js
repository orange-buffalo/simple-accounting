import MyProfile from '@/views/profile/MyProfile';
import { onGet } from '@/stories/utils/stories-api-mocks';

export default {
  title: 'Pages/MyProfile',
  parameters: {
    fullWidth: true,
  },
};

export const WithGoogleDrive = () => ({
  components: { MyProfile },
  template: '<MyProfile/>',
  beforeCreate() {
    onGet('/api/profile')
      .reply(200, {
        userName: 'fry',
        documentsStorage: 'google-drive',
        i18n: {
          locale: 'en',
          language: 'en',
        },
      });
    onGet('/api/storage/google-drive/status')
      .reply(200, {
        folderId: '42',
        folderName: 'simple-accounting',
        authorizationRequired: false,
      });
  },
});
