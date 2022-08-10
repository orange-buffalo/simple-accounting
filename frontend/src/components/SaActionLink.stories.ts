import { action } from '@storybook/addon-actions';
import SaActionLink from '@/components/SaActionLink.vue';
import { defineStory } from '@/__storybook__/sa-storybook';
import { waitForText } from '@/__storybook__/screenshots';

// noinspection JSUnusedGlobalSymbols
export default {
  title: 'Components/SaActionLink',
};

// noinspection JSUnusedGlobalSymbols
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
