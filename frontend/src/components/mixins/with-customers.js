import useCustomers from '@/components/customer/useCustomers';
import { findByIdOrEmpty } from '@/components/utils/utils';

export default {
  data() {
    return {
      customers: [],
    };
  },

  async created() {
    const { customers } = useCustomers();
    this.customers = customers;
  },

  computed: {
    customerById() {
      return customerId => findByIdOrEmpty(this.customers, customerId);
    },
  },
};
