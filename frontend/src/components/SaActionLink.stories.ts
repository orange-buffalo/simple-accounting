// noinspection JSUnusedGlobalSymbols

import { action } from '@storybook/addon-actions';
import SaActionLink from '@/components/SaActionLink.vue';
import { defineStory } from '@/__storybook__/sa-storybook';
import { waitForText } from '@/__storybook__/screenshots';

export default {
  title: 'Components/Basic/SaActionLink',
};

export const Default = defineStory(() => ({
  components: { SaActionLink },
  methods: {
    onClick() {
      action('on-click')();
    },
  },
  template: '<SaActionLink icon="draft" @click="onClick">Link</SaActionLink>',
}), {
  screenshotPreparation: waitForText('Link'),
});
