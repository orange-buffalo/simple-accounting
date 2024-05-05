import { onMounted, onUnmounted } from 'vue';
import { profileApi, useCancellableRequest } from '@/services/api';
import { fetchMock } from '@/__storybook__/api-mocks';

export default {
  title: 'Components/Other/ErrorHandler',
};

export const FatalApiError = () => ({
  setup() {
    onMounted(async () => {
      fetchMock.get('api/profile', { status: 500 });
      await profileApi.getProfile();
    });

    onUnmounted(() => {
      fetchMock.reset();
    });
  },
  template: `
    <h3>Fatal error from API</h3>
  `,
});

export const CancelledRequest = () => ({
  setup() {
    const {
      cancellableRequestConfig,
      cancelRequest,
    } = useCancellableRequest();
    onMounted(async () => {
      cancelRequest();
      fetchMock.get('api/profile', { delay: 20000 });
      await profileApi.getProfile(cancellableRequestConfig);
    });

    onUnmounted(() => {
      fetchMock.reset();
    });
  },
  template: `
    <h3>Cancelled API request</h3>
  `,
});

export const ApiAuthError = () => ({
  setup() {
    onMounted(async () => {
      fetchMock.get('api/profile', { status: 401 });
      fetchMock.post('/api/auth/token', {
      status: 401,
    });
      await profileApi.getProfile();
    });

    onUnmounted(() => {
      fetchMock.reset();
    });
  },
  template: `
    <h3>API Auth error</h3>
  `,
});
