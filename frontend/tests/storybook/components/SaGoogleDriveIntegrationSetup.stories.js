import SaGoogleDriveIntegrationSetup from '@/components/documents/storage/SaGoogleDriveIntegrationSetup';
import pushNotifications from '@/services/push-notifications';
import SaGoogleDriveIntegrationSetupStoriesAuthorizationCallback
  from '../components/SaGoogleDriveIntegrationSetupStoriesAuthorizationCallback';
import { onGet } from '../utils/stories-api-mocks';
import { NO_STORYSHOTS_STORY, removeSvgAnimations, storyshotsStory } from '../utils/stories-utils';

// noinspection JSUnusedGlobalSymbols
export default {
  title: 'Components|SaGoogleDriveIntegrationSetup',
};

export const InitialLoading = () => ({
  components: { SaGoogleDriveIntegrationSetup },
  template: '<SaGoogleDriveIntegrationSetup />',
  beforeCreate() {
    onGet('api/storage/google-drive/status')
      .neverEndingRequest();
  },
});
InitialLoading.story = storyshotsStory({
  async setup(page) {
    await removeSvgAnimations(page);
  },
});

// noinspection JSUnusedGlobalSymbols
export const ActiveIntegration = () => ({
  components: { SaGoogleDriveIntegrationSetup },
  template: '<SaGoogleDriveIntegrationSetup />',
  beforeCreate() {
    onGet('api/storage/google-drive/status')
      .successJson({
        folderId: '42',
        folderName: 'simple-accounting',
        authorizationRequired: false,
      });
  },
});

// noinspection JSUnusedGlobalSymbols
export const AuthorizationRequired = () => ({
  components: { SaGoogleDriveIntegrationSetup },
  template: '<SaGoogleDriveIntegrationSetup />',
  beforeCreate() {
    onGet('api/storage/google-drive/status')
      .successJson({
        authorizationRequired: true,
        authorizationUrl: 'https://google.com',
      });
  },
});

// noinspection JSUnusedGlobalSymbols
export const AuthorizationFailed = () => ({
  components: { SaGoogleDriveIntegrationSetup },
  template: '<SaGoogleDriveIntegrationSetup />',
  beforeCreate() {
    onGet('api/storage/google-drive/status')
      .successJson({
        authorizationRequired: true,
        authorizationUrl: 'https://google.com',
      });
  },
  mounted() {
    setTimeout(() => {
      pushNotifications.pushEvent('storage.google-drive.auth', {
        authorizationRequired: true,
        authorizationUrl: 'https://google.com',
      });
    }, 0);
  },
});

export const AuthorizationCallbacks = () => ({
  components: { SaGoogleDriveIntegrationSetupStoriesAuthorizationCallback },
  template: '<SaGoogleDriveIntegrationSetupStoriesAuthorizationCallback />',
  beforeCreate() {
    onGet('api/storage/google-drive/status')
      .successJson({
        authorizationRequired: true,
        authorizationUrl: 'https://google.com',
      });
  },
});
AuthorizationCallbacks.story = NO_STORYSHOTS_STORY;
