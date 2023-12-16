// noinspection JSUnusedGlobalSymbols

import {
  neverEndingGetRequest,
  onGetToDefaultWorkspacePath,
} from '@/__storybook__/api-mocks';
import SaCustomerOutput from '@/components/customer/SaCustomerOutput.vue';
import { defineStory } from '@/__storybook__/sa-storybook';
import { waitForText } from '@/__storybook__/screenshots';
import { storybookData } from '@/__storybook__/storybook-data';

export default {
  title: 'Components/Domain/Customer/SaCustomerOutput',
};

export const Loaded = defineStory(() => ({
  components: { SaCustomerOutput },
  template: '<SaCustomerOutput :customer-id="storybookData.customers.governmentOfEarth.id"/>',
  ...storybookData.storyComponentConfig,
}), {
  screenshotPreparation: waitForText(storybookData.customers.governmentOfEarth.name),
});

export const Loading = defineStory(() => ({
  components: { SaCustomerOutput },
  template: '<SaCustomerOutput :customer-id="7"/>',
  setup() {
    onGetToDefaultWorkspacePath('/customers/7', {}, neverEndingGetRequest);
  },
}));
