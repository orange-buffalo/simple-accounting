import OAuthCallbackPage from '@/views/auth/OAuthCallbackPage';
import { onPost } from '../utils/stories-api-mocks';
import { removeSvgAnimations, setViewportHeight, storyshotsStory } from '../utils/stories-utils';

// noinspection JSUnusedGlobalSymbols
export default {
  title: 'Pages|OAuthCallbackPage',
  parameters: {
    fullWidth: true,
    ...storyshotsStory({
      async setup(page) {
        await setViewportHeight(page, 400);
      },
    }),
  },
};

export const Loading = () => ({
  components: { OAuthCallbackPage },
  template: '<OAuthCallbackPage/>',
  beforeCreate() {
    onPost('/api/auth/oauth2/callback')
      .neverEndingRequest();
  },
});
Loading.story = storyshotsStory({
  async setup(page) {
    await removeSvgAnimations(page);
    await setViewportHeight(page, 400);
  },
});

// noinspection JSUnusedGlobalSymbols
export const Success = () => ({
  components: { OAuthCallbackPage },
  template: '<OAuthCallbackPage/>',
  beforeCreate() {
    onPost('/api/auth/oauth2/callback')
      .successJson({});
  },
});

// noinspection JSUnusedGlobalSymbols
export const Failure = () => ({
  components: { OAuthCallbackPage },
  template: '<OAuthCallbackPage/>',
  beforeCreate() {
    onPost('/api/auth/oauth2/callback')
      .intercept((req, res) => res.status(400)
        .json({ errorId: '3213-f23df-2dsfds-232' }));
  },
});
