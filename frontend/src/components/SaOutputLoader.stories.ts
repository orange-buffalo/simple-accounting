import SaOutputLoader from '@/components/SaOutputLoader.vue';

// noinspection JSUnusedGlobalSymbols
export default {
  title: 'Components/SaOutputLoader',
};

export const Loading = () => ({
  components: { SaOutputLoader },
  template: '<SaOutputLoader loading>Content</SaOutputLoader>',
});

// noinspection JSUnusedGlobalSymbols
export const Loaded = () => ({
  components: { SaOutputLoader },
  template: '<SaOutputLoader :loading="false">Content</SaOutputLoader>',
});
