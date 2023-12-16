// noinspection JSUnusedGlobalSymbols

import OAuthCallbackPage from '@/pages/oauth-callback/OAuthCallbackPage.vue';
import { fetchMock, neverEndingPostRequest } from '@/__storybook__/api-mocks';
import { defineStory } from '@/__storybook__/sa-storybook';
import { allOf, disableIconsSvgAnimations, waitForText } from '@/__storybook__/screenshots';

export default {
  title: 'Pages/OAuthCallbackPage',
};

export const Loading = defineStory(() => ({
  components: { OAuthCallbackPage },
  template: '<OAuthCallbackPage />',
  setup() {
    fetchMock.post('/api/auth/oauth2/callback', {}, neverEndingPostRequest);
  },
}), {
  screenshotPreparation: allOf(
    waitForText('We are completing the authorization'),
    disableIconsSvgAnimations(),
  ),
});

export const Success = defineStory(() => ({
  components: { OAuthCallbackPage },
  template: '<OAuthCallbackPage />',
  setup() {
    fetchMock.post('/api/auth/oauth2/callback', {});
  },
}), {
  screenshotPreparation: waitForText('Authorization successfully completed'),
});

export const Failure = defineStory(() => ({
  components: { OAuthCallbackPage },
  template: '<OAuthCallbackPage />',
  setup() {
    fetchMock.post('/api/auth/oauth2/callback', {
      status: 400,
      body: { errorId: '3213-f23df-2dsfds-232' },
    });
  },
}), {
  screenshotPreparation: waitForText('Authorization failed'),
});
