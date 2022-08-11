// noinspection JSUnusedGlobalSymbols

import { fetchMock, defaultWorkspacePath, neverEndingGetRequest } from '@/__storybook__/api-mocks';
import SaGeneralTaxOutput from '@/components/general-tax/SaGeneralTaxOutput.vue';
import { defineStory } from '@/__storybook__/sa-storybook';
import { disableOutputLoaderAnimations, waitForText } from '@/__storybook__/screenshots';

export default {
  title: 'Components/SaGeneralTaxOutput',
};

export const Loaded = defineStory(() => ({
  components: { SaGeneralTaxOutput },
  template: '<SaGeneralTaxOutput :general-tax-id="7"/>',
  beforeCreate() {
    fetchMock.get(defaultWorkspacePath('/general-taxes/7'), {
      title: 'Tax Name',
    });
  },
}), {
  screenshotPreparation: waitForText('Tax Name'),
});

export const Loading = defineStory(() => ({
  components: { SaGeneralTaxOutput },
  template: '<SaGeneralTaxOutput :general-tax-id="7"/>',
  beforeCreate() {
    fetchMock.get(defaultWorkspacePath('/general-taxes/7'), {}, neverEndingGetRequest);
  },
}), {
  screenshotPreparation: disableOutputLoaderAnimations(),
});
