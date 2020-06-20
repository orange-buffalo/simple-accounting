import SaCustomerOutput from '@/components/customer/SaCustomerOutput';
import { onGetToWorkspacePath, apiPage } from '@/stories/utils/stories-api-mocks';

export default {
  title: 'Components/SaCustomerOutput',
};

export const Basic = () => ({
  components: { SaCustomerOutput },
  template: '<SaCustomerOutput :customer-id="7"/>',
  beforeCreate() {
    onGetToWorkspacePath('customers')
      .successJson(apiPage([{
        id: 7,
        name: 'Customer Name',
      }]));
  },
});
