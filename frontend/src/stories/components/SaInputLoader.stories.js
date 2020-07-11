import SaInputLoader from '@/components/SaInputLoader';

export default {
  title: 'Components|SaInputLoader',
};

const delay = 1000;

function createStory(componentConfig) {
  return () => ({
    components: { SaInputLoader },
    data() {
      return {
        loading: false,
        error: false,
      };
    },
    template: '<SaInputLoader :loading="loading" :error="error" style="width: 300px"><ElInput/></SaInputLoader>',
    ...componentConfig,
  });
}

export const Loaded = createStory({
  created() {
    this.loading = false;
  },
});

export const Loading = createStory({
  created() {
    this.loading = true;
  },
});

export const Error = createStory({
  created() {
    this.error = true;
  },
});

export const DelayedLoading = createStory({
  created() {
    this.loading = true;
    setTimeout(() => {
      this.loading = false;
    }, delay);
  },
});

export const DelayedError = createStory({
  created() {
    this.loading = true;
    setTimeout(() => {
      this.error = true;
      this.loading = false;
    }, delay);
  },
});
