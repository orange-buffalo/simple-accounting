// noinspection JSUnusedGlobalSymbols

import SaNotesInput from '@/components/notes-input/SaNotesInput.vue';
import { defineStory } from '@/__storybook__/sa-storybook';
import { allOf, waitForText } from '@/__storybook__/screenshots';

export default {
  title: 'Components/SaNotesInput',
};

export const Default = defineStory(() => ({
  components: { SaNotesInput },
  data: () => ({
    notes: 'Some notes *with formatting*',
  }),
  template: `
    <h4>Preloaded value</h4>
    <SaNotesInput v-model="notes"
                placeholder="Placeholder"
    />
    <br />
    <pre>{{ notes }}</pre>
    <h4>Empty value</h4>
    <SaNotesInput placeholder="Provide value" />
  `,
}), {
  screenshotPreparation: allOf(
    waitForText('with formatting', 'em'),
    waitForText('Preview'),
  ),
  useRealTime: true,
});
