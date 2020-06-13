import SaGoogleDriveIntegrationSetup from '@/components/documents/storage/SaGoogleDriveIntegrationSetup';
import SaGoogleDriveIntegrationSetupStoriesAuthorizationCallback
  from '@/stories/components/SaGoogleDriveIntegrationSetupStoriesAuthorizationCallback';
import { onGet } from '@/stories/utils/stories-api-mocks';
import pushNotifications from '@/services/push-notifications';

export default {
  title: 'Components/SaGoogleDriveIntegrationSetup',
};

export const InitialLoading = () => ({
  components: { SaGoogleDriveIntegrationSetup },
  template: '<SaGoogleDriveIntegrationSetup />',
  beforeCreate() {
    onGet('api/storage/google-drive/status')
      .neverEndingRequest();
  },
});

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
