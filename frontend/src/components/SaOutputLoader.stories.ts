import SaOutputLoader from '@/components/SaOutputLoader.vue';
import { defineStory } from '@/__storybook__/sa-storybook';
import { disableOutputLoaderAnimations, waitForText } from '@/__storybook__/screenshots';

// noinspection JSUnusedGlobalSymbols
export default {
  title: 'Components/SaOutputLoader',
};

export const Loading = defineStory(() => ({
  components: { SaOutputLoader },
  template: '<SaOutputLoader loading>Content</SaOutputLoader>',
}), {
  screenshotPreparation: disableOutputLoaderAnimations(),
});

// noinspection JSUnusedGlobalSymbols
export const Loaded = defineStory(() => ({
  components: { SaOutputLoader },
  template: '<SaOutputLoader :loading="false">Content</SaOutputLoader>',
}), {
  screenshotPreparation: waitForText('Content'),
});
