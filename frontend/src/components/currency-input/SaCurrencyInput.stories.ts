// noinspection JSUnusedGlobalSymbols

import SaCurrencyInput from '@/components/currency-input/SaCurrencyInput.vue';
import { defineStory } from '@/__storybook__/sa-storybook';
import { allOf, clickOnElement, waitForText } from '@/__storybook__/screenshots';
import { defaultWorkspacePath, fetchMock } from '@/__storybook__/api-mocks';

export default {
  title: 'Components/Basic/SaCurrencyInput',
};

export const Default = defineStory(() => ({
  components: { SaCurrencyInput },
  data: () => ({
    selectedCurrency: undefined,
  }),
  beforeCreate() {
    fetchMock.get(defaultWorkspacePath('/statistics/currencies-shortlist'), [
      'EUR', 'AUD',
    ]);
  },
  template: `
    <div class="row">
    <div class="col-lg-4">
      <h4>Default</h4>
      <SaCurrencyInput v-model="selectedCurrency" />
      <br /><br />
      {{ selectedCurrency }}
    </div>
    <div class="col-lg-4">
      <h4>Disabled</h4>
      <SaCurrencyInput model-value="USD" disabled />
    </div>
    </div>
  `,
}), {
  screenshotPreparation: allOf(
    clickOnElement('.select-trigger'),
    waitForText('Recently Used Currencies'),
  ),
});
