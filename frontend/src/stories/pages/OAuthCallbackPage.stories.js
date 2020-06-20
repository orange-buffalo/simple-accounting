import OAuthCallbackPage from '@/views/auth/OAuthCallbackPage';
import { onPost } from '@/stories/utils/stories-api-mocks';

export default {
  title: 'Pages/OAuthCallbackPage',
  parameters: {
    fullWidth: true,
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

export const Success = () => ({
  components: { OAuthCallbackPage },
  template: '<OAuthCallbackPage/>',
  beforeCreate() {
    onPost('/api/auth/oauth2/callback')
      .successJson({});
  },
});

export const Failure = () => ({
  components: { OAuthCallbackPage },
  template: '<OAuthCallbackPage/>',
  beforeCreate() {
    onPost('/api/auth/oauth2/callback')
      .intercept((req, res) => res.status(400)
        .json({ errorId: '3213-f23df-2dsfds-232' }));
  },
});
