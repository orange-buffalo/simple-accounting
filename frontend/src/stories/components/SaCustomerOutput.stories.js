import SaCustomerOutput from '@/components/customer/SaCustomerOutput';
import mainConfig from '@/setup/setup-app';
import { api } from '@/services/api';
// eslint-disable-next-line import/no-extraneous-dependencies
import MockAdapter from 'axios-mock-adapter';

const { app } = mainConfig;
app.store.commit('workspaces/setCurrentWorkspace', { id: 42 });

const mock = new MockAdapter(api);

export default {
  title: 'Components/SaCustomerOutput',
};

export const Basic = () => ({
  components: { SaCustomerOutput },
  template: '<SaCustomerOutput :customer-id="7"/>',
  beforeCreate() {
    mock.reset();
    mock.onGet(new RegExp('/api/workspaces/42/customers?.*'))
      .reply(200, {
        data: [{
          id: 7,
          name: 'Customer Name',
        }],
      });
  },
});
