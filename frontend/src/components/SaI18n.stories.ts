// noinspection JSUnusedGlobalSymbols

import SaI18n from '@/components/SaI18n.vue';
import SaIcon from '@/components/SaIcon.vue';
import { defineStory } from '@/__storybook__/sa-storybook';
import { waitForText } from '@/__storybook__/screenshots';

export default {
  title: 'Components/SaI18n',
};

export const JustText = defineStory(() => ({
  components: { SaI18n },
  template: '<SaI18n message="Message text" />',
}), {
  screenshotPreparation: waitForText('Message text'),
});

export const WithSlot = defineStory(() => ({
  components: {
    SaI18n,
    SaIcon,
  },
  template: `
    <SaI18n message="Message {default} with icon">
    <SaIcon icon="draft" />
    </SaI18n>
  `,
}), {
  screenshotPreparation: waitForText('with icon'),
});
