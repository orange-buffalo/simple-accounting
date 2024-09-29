// noinspection JSUnusedGlobalSymbols

import { neverEndingGetRequest, onGetToDefaultWorkspacePath } from '@/__storybook__/api-mocks';
import SaCustomerInput from '@/components/customer/SaCustomerInput.vue';
import { defineStory } from '@/__storybook__/sa-storybook';
import { storybookData } from '@/__storybook__/storybook-data';

export default {
  title: 'Components/Domain/Customer/SaCustomerInput',
};

export const Default = defineStory(() => ({
  components: { SaCustomerInput },
  data: () => ({
    presetCustomerId: storybookData.customers.democraticOrderOfPlanets.id,
    initiallyEmptyCustomerId: undefined,
  }),
  template: `
    <h4>Empty value</h4>
    <SaCustomerInput v-model="initiallyEmptyCustomerId"
                     placeholder="Please select customer"
                     clearable
                     id="initially-empty-select"
    />
    <br />
    {{ initiallyEmptyCustomerId }}

    <h4>Preset value</h4>
    <SaCustomerInput v-model="presetCustomerId"
                     id="preset-select"
    />
    <br />
    {{ presetCustomerId }}
  `,
  ...storybookData.storyComponentConfig,
}));

export const Loading = defineStory(() => ({
  components: { SaCustomerInput },
  template: '<SaCustomerInput />',
  setup() {
    onGetToDefaultWorkspacePath('/customers', {}, neverEndingGetRequest);
  },
}));
