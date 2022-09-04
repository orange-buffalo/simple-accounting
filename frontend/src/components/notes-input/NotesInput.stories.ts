// noinspection JSUnusedGlobalSymbols

import NotesInput from '@/components/notes-input/NotesInput.vue';
import { defineStory } from '@/__storybook__/sa-storybook';
import { allOf, waitForText } from '@/__storybook__/screenshots';

export default {
  title: 'Components/NotesInput',
};

export const Default = defineStory(() => ({
  components: { NotesInput },
  data: () => ({
    notes: 'Some notes *with formatting*',
  }),
  template: `
    <h4>Preloaded value</h4>
    <NotesInput v-model="notes"
                placeholder="Placeholder"
    />
    <br />
    <pre>{{ notes }}</pre>
    <h4>Empty value</h4>
    <NotesInput placeholder="Provide value" />
  `,
}), {
  screenshotPreparation: allOf(
    waitForText('with formatting', 'em'),
    waitForText('Preview'),
  ),
  useRealTime: true,
});
