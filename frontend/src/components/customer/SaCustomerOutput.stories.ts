// noinspection JSUnusedGlobalSymbols

import { fetchMock, defaultWorkspacePath, neverEndingGetRequest } from '@/__storybook__/api-mocks';
import SaCustomerOutput from '@/components/customer/SaCustomerOutput.vue';
import { defineStory } from '@/__storybook__/sa-storybook';
import { disableOutputLoaderAnimations, waitForText } from '@/__storybook__/screenshots';

export default {
  title: 'Components/SaCustomerOutput',
};

export const Loaded = defineStory(() => ({
  components: { SaCustomerOutput },
  template: '<SaCustomerOutput :customer-id="7"/>',
  beforeCreate() {
    fetchMock.get(defaultWorkspacePath('/customers/7'), {
      name: 'Customer Name',
    });
  },
}), {
  screenshotPreparation: waitForText('Customer Name'),
});

export const Loading = defineStory(() => ({
  components: { SaCustomerOutput },
  template: '<SaCustomerOutput :customer-id="7"/>',
  beforeCreate() {
    fetchMock.get(defaultWorkspacePath('/customers/7'), {}, neverEndingGetRequest);
  },
}), {
  screenshotPreparation: disableOutputLoaderAnimations(),
});
