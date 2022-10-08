// noinspection JSUnusedGlobalSymbols

import SaOutputLoader from '@/components/SaOutputLoader.vue';
import { defineStory } from '@/__storybook__/sa-storybook';
import { waitForText } from '@/__storybook__/screenshots';

export default {
  title: 'Components/Basic/SaOutputLoader',
};

export const Default = defineStory(() => ({
  components: { SaOutputLoader },
  template: `
    <h4>Loading</h4>
    <SaOutputLoader loading>Content</SaOutputLoader>

    <h4>Loaded</h4>
    <SaOutputLoader :loading="false">Content</SaOutputLoader>
  `,
}), {
  screenshotPreparation: waitForText('Content'),
});
