import SaGoogleDriveIntegrationStatus from '@/components/documents/storage/SaGoogleDriveIntegrationStatus';
import SaGoogleDriveIntegrationStatusStoriesAuthorizationCallback
  from '@/stories/components/SaGoogleDriveIntegrationStatusStoriesAuthorizationCallback';
import { onGet } from '@/stories/utils/stories-api-mocks';
import pushNotifications from '@/services/push-notifications';

export default {
  title: 'Components/SaGoogleDriveIntegrationStatus',
};

export const InitialLoading = () => ({
  components: { SaGoogleDriveIntegrationStatus },
  template: '<SaGoogleDriveIntegrationStatus />',
  beforeCreate() {
    onGet('api/storage/google-drive/status')
      .neverEndingRequest();
  },
});

export const ActiveIntegration = () => ({
  components: { SaGoogleDriveIntegrationStatus },
  template: '<SaGoogleDriveIntegrationStatus />',
  beforeCreate() {
    onGet('api/storage/google-drive/status')
      .reply(200, {
        folderId: '42',
        folderName: 'simple-accounting',
        authorizationRequired: false,
      });
  },
});

export const AuthorizationRequired = () => ({
  components: { SaGoogleDriveIntegrationStatus },
  template: '<SaGoogleDriveIntegrationStatus />',
  beforeCreate() {
    onGet('api/storage/google-drive/status')
      .reply(200, {
        authorizationRequired: true,
        authorizationUrl: 'https://google.com',
      });
  },
});

export const AuthorizationFailed = () => ({
  components: { SaGoogleDriveIntegrationStatus },
  template: '<SaGoogleDriveIntegrationStatus />',
  beforeCreate() {
    onGet('api/storage/google-drive/status')
      .reply(200, {
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
  components: { SaGoogleDriveIntegrationStatusStoriesAuthorizationCallback },
  template: '<SaGoogleDriveIntegrationStatusStoriesAuthorizationCallback />',
  beforeCreate() {
    onGet('api/storage/google-drive/status')
      .reply(200, {
        authorizationRequired: true,
        authorizationUrl: 'https://google.com',
      });
  },
});
