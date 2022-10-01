// noinspection JSUnusedGlobalSymbols

import { action } from '@storybook/addon-actions';
import { defineStory } from '@/__storybook__/sa-storybook';
import { allOf, waitForText } from '@/__storybook__/screenshots';
import MyProfile from '@/pages/my-profile/MyProfile.vue';
import { fetchMock } from '@/__storybook__/api-mocks';

export default {
  title: 'Pages/MyProfile',
};

export const WithGoogleDrive = defineStory(() => ({
  components: { MyProfile },
  template: '<MyProfile/>',
  beforeCreate() {
    fetchMock.get('/api/profile', {
      userName: 'fry',
      documentsStorage: 'google-drive',
      i18n: {
        locale: 'en',
        language: 'en',
      },
    });
    fetchMock.get('/api/storage/google-drive/status', {
      folderId: '42',
      folderName: 'simple-accounting',
      authorizationRequired: false,
    });
    fetchMock.put('/api/profile', (_, req) => {
      action('update-profile-request')(JSON.parse(req.body as string));
      return {};
    });
  },
}), {
  asPage: true,
  screenshotPreparation: allOf(
    waitForText('Google Drive integration is active'),
    waitForText('Interface Language'),
  ),
});
