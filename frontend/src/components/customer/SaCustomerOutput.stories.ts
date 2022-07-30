// noinspection JSUnusedGlobalSymbols

import { fetchMock, defaultWorkspacePath, neverEndingGetRequest } from '@/__storybook__/api-mocks';
import SaCustomerOutput from '@/components/customer/SaCustomerOutput.vue';

export default {
  title: 'Components/SaCustomerOutput',
};

export const Loaded = () => ({
  components: { SaCustomerOutput },
  template: '<SaCustomerOutput :customer-id="7"/>',
  beforeCreate() {
    fetchMock.get(defaultWorkspacePath('/customers/7'), {
      name: 'Customer Name',
    });
  },
});

export const Loading = () => ({
  components: { SaCustomerOutput },
  template: '<SaCustomerOutput :customer-id="7"/>',
  beforeCreate() {
    fetchMock.get(defaultWorkspacePath('/customers/7'), {}, neverEndingGetRequest);
  },
});
