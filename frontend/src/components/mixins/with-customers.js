import { mapState } from 'vuex';
import { isNil } from 'lodash';
import { api } from '@/services/api';

export const withCustomers = {
  data() {
    return {
      customers: [],
    };
  },

  async created() {
    this.customers = await api.pageRequest(`/workspaces/${this.$_withCustomers_currentWorkspace.id}/customers`)
      .eager()
      .getPageData();
  },

  computed: {
    ...mapState({
      $_withCustomers_currentWorkspace: state => state.workspaces.currentWorkspace,
    }),

    customerById() {
      return (customerId) => {
        const customer = this.customers.find(customer => customer.id === customerId);
        return isNil(customer) ? {} : customer;
      };
    },
  },
};

export default withCustomers;
