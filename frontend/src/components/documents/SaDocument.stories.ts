// noinspection JSUnusedGlobalSymbols

import { action } from '@storybook/addon-actions';
import { onGetToDefaultWorkspacePath } from '@/__storybook__/api-mocks';
import SaDocument from '@/components/documents/SaDocument.vue';
import { defineStory } from '@/__storybook__/sa-storybook';

export default {
  title: 'Components/Basic/Documents/SaDocument',
};

export const Loading = defineStory(() => ({
  components: { SaDocument },
  template: '<SaDocument loading style="width: 400px" />',
}));

export const Loaded = () => ({
  components: { SaDocument },
  beforeCreate() {
    onGetToDefaultWorkspacePath('/documents/42/download-token', {
      token: 'token66',
    });
  },
  template: `
    <SaDocument document-name="Service Agreement.pdf"
                :document-id="42"
                :document-size-in-bytes="832992"
                style="width: 400px"
    />
  `,
});

export const Removable = () => ({
  components: { SaDocument },
  data() {
    return {
      onRemoved: action('removed'),
    };
  },
  template: `
    <SaDocument document-name="Service Agreement.pdf"
                removable
                @removed="onRemoved"
                :document-id="42"
                :document-size-in-bytes="832992"
                style="width: 400px"
    />
  `,
});

export const InProgress = () => ({
  components: { SaDocument },
  template: `
    <SaDocument document-name="Service Agreement.pdf"
                in-progress :progress="34"
                style="width: 400px"
    />
  `,
});

export const CustomExtra = () => ({
  components: { SaDocument },
  template: `
    <SaDocument document-name="Service Agreement.pdf"
                style="width: 400px"
    >
    <template #extras>Custom status</template>
    </SaDocument>
  `,
});
