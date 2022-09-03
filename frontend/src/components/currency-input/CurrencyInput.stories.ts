// noinspection JSUnusedGlobalSymbols

import CurrencyInput from '@/components/currency-input/CurrencyInput.vue';
import { defineStory } from '@/__storybook__/sa-storybook';
import { allOf, clickOnElement, waitForText } from '@/__storybook__/screenshots';
import { defaultWorkspacePath, fetchMock } from '@/__storybook__/api-mocks';

export default {
  title: 'Components/CurrencyInput',
};

export const Default = defineStory(() => ({
  components: { CurrencyInput },
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
      <CurrencyInput v-model="selectedCurrency" />
      <br /><br />
      {{ selectedCurrency }}
    </div>
    <div class="col-lg-4">
      <h4>Disabled</h4>
      <CurrencyInput model-value="USD" disabled />
    </div>
    </div>
  `,
}), {
  screenshotPreparation: allOf(
    clickOnElement('.select-trigger'),
    waitForText('Recently Used Currencies'),
  ),
});
