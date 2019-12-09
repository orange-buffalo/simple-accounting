import { mapState } from 'vuex';
import { api } from '@/services/api';

export default {
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
        const customer = this.customers.find(it => it.id === customerId);
        return customer == null ? {} : customer;
      };
    },
  },
};
