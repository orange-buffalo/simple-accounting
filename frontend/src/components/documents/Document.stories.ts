// noinspection JSUnusedGlobalSymbols

import { action } from '@storybook/addon-actions';
import { onGetToDefaultWorkspacePath } from '@/__storybook__/api-mocks';
import Document from '@/components/documents/Document.vue';
import { defineStory } from '@/__storybook__/sa-storybook';

export default {
  title: 'Components/Document',
};

export const Loading = defineStory(() => ({
  components: { Document },
  template: '<Document loading style="width: 400px" />',
}));

export const Loaded = () => ({
  components: { Document },
  beforeCreate() {
    onGetToDefaultWorkspacePath('/documents/42/download-token', {
      token: 'token66',
    });
  },
  template: `
    <Document document-name="Service Agreement.pdf"
              :document-id="42"
              :document-size-in-bytes="832992"
              style="width: 400px"
    />
  `,
});

export const Removable = () => ({
  components: { Document },
  data() {
    return {
      onRemoved: action('removed'),
    };
  },
  template: `
    <Document document-name="Service Agreement.pdf"
              removable
              @removed="onRemoved"
              :document-id="42"
              :document-size-in-bytes="832992"
              style="width: 400px"
    />
  `,
});

export const InProgress = () => ({
  components: { Document },
  template: `
    <Document document-name="Service Agreement.pdf"
              in-progress :progress="34"
              style="width: 400px"
    />
  `,
});

export const CustomExtra = () => ({
  components: { Document },
  template: `
    <Document document-name="Service Agreement.pdf"
              style="width: 400px"
    >
      <template #extras>Custom status</template>
    </Document>
  `,
});
