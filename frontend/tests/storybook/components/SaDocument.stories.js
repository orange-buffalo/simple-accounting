import { action } from '@storybook/addon-actions';
import SaDocument from '@/components/documents/SaDocument';
import { pauseAndResetDocumentLoaderAnimation, storyshotsStory } from '../utils/stories-utils';

// noinspection JSUnusedGlobalSymbols
export default {
  title: 'Components|SaDocument',
};

export const Loading = () => ({
  components: { SaDocument },
  template: '<SaDocument loading style="width: 400px" />',
});
Loading.story = storyshotsStory({
  async setup(page) {
    await pauseAndResetDocumentLoaderAnimation(page);
  },
});

// noinspection JSUnusedGlobalSymbols
export const Loaded = () => ({
  components: { SaDocument },
  template: '<SaDocument document-name="Service Agreement.pdf" '
    + ':document-id="42" :document-size-in-bytes="832992" style="width: 400px" />',
});

// noinspection JSUnusedGlobalSymbols
export const Removable = () => ({
  components: { SaDocument },
  data() {
    return {
      onRemoved: action('removed'),
    };
  },
  template: '<SaDocument document-name="Service Agreement.pdf" removable @removed="onRemoved" '
    + ':document-id="42" :document-size-in-bytes="832992" style="width: 400px" />',
});

// noinspection JSUnusedGlobalSymbols
export const InProgress = () => ({
  components: { SaDocument },
  template: '<SaDocument document-name="Service Agreement.pdf" in-progress :progress="34" '
    + 'style="width: 400px"><template #extras>Custom loading status</template></SaDocument>',
});
