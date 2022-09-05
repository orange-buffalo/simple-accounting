// noinspection JSUnusedGlobalSymbols

import SaI18n from '@/components/SaI18n.vue';
import SaIcon from '@/components/SaIcon.vue';
import { defineStory } from '@/__storybook__/sa-storybook';
import { allOf, waitForText } from '@/__storybook__/screenshots';

export default {
  title: 'Components/SaI18n',
};

export const Default = defineStory(() => ({
  components: {
    SaI18n,
    SaIcon,
  },
  template: `
    <h4>Just Text</h4>
    <SaI18n message="Message text" />

    <h4>WithSlot</h4>
    <SaI18n message="Message {default} with icon">
    <SaIcon icon="draft" />
    </SaI18n>
  `,
}), {
  screenshotPreparation: allOf(
    waitForText('Message text'),
    waitForText('with icon'),
  ),
});
