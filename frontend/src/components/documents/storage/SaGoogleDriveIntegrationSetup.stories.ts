// noinspection JSUnusedGlobalSymbols

import type { GoogleDriveStorageIntegrationStatus } from '@/services/api';
import { pushEvent } from '@/services/push-notifications';
import {
  allOf, clickOnElement, disableIconsSvgAnimations, skipWhenSucceededOnce, waitForText,
} from '@/__storybook__/screenshots';
import { fetchMock } from '@/__storybook__/api-mocks';
import SaGoogleDriveIntegrationSetup from '@/components/documents/storage/SaGoogleDriveIntegrationSetup.vue';
import { defineStory } from '@/__storybook__/sa-storybook';

export default {
  title: 'Components/SaGoogleDriveIntegrationSetup',
};

export const AuthorizationRequired = defineStory(() => ({
  components: { SaGoogleDriveIntegrationSetup },
  beforeCreate() {
    fetchMock.get('/api/storage/google-drive/status', {
      authorizationRequired: true,
      authorizationUrl: '/',
    } as GoogleDriveStorageIntegrationStatus);
  },
  template: '<SaGoogleDriveIntegrationSetup  />',
}), {
  screenshotPreparation: waitForText('Authorization required'),
});

export const InProgress = defineStory(() => ({
  components: { SaGoogleDriveIntegrationSetup },
  beforeCreate() {
    fetchMock.get('/api/storage/google-drive/status', {
      authorizationRequired: true,
      authorizationUrl: '/',
    } as GoogleDriveStorageIntegrationStatus);
  },
  template: '<SaGoogleDriveIntegrationSetup  />',
}), {
  screenshotPreparation: allOf(
    clickOnElement('.el-button'),
    waitForText('Authorization in progress'),
    disableIconsSvgAnimations(),
  ),
});

export const Authorized = defineStory(() => ({
  components: { SaGoogleDriveIntegrationSetup },
  beforeCreate() {
    fetchMock.get('/api/storage/google-drive/status', {
      authorizationRequired: false,
      folderId: 'xxxyyyzzz',
      folderName: 'simple-accounting-data',
    } as GoogleDriveStorageIntegrationStatus);
  },
  template: '<SaGoogleDriveIntegrationSetup  />',
}), {
  screenshotPreparation: waitForText('Google Drive integration is active'),
});

export const Failed = defineStory(() => ({
  components: { SaGoogleDriveIntegrationSetup },
  beforeCreate() {
    fetchMock.get('/api/storage/google-drive/status', {
      authorizationRequired: true,
      authorizationUrl: '/',
    } as GoogleDriveStorageIntegrationStatus);
  },
  template: '<SaGoogleDriveIntegrationSetup  />',
}), {
  screenshotPreparation: allOf(
    clickOnElement('.el-button'),
    waitForText('Authorization in progress'),
    skipWhenSucceededOnce(() => {
      pushEvent('storage.google-drive.auth', {
        authorizationRequired: true,
        authorizationUrl: '/',
      });
      return true;
    }),
    waitForText('Application authorization failed'),
  ),
});
