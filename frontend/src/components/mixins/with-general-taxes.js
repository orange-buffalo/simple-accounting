import { mapState } from 'vuex';
import { api } from '@/services/api';

export default {
  data() {
    return {
      generalTaxes: [],
    };
  },

  async created() {
    this.generalTaxes = await api
      .pageRequest(`/workspaces/${this.$_withGeneralTaxes_currentWorkspace.id}/general-taxes`)
      .eager()
      .getPageData();
  },

  computed: {
    ...mapState({
      $_withGeneralTaxes_currentWorkspace: state => state.workspaces.currentWorkspace,
    }),

    generalTaxById() {
      return (taxId) => {
        const tax = this.generalTaxes.find(it => it.id === taxId);
        return tax == null ? {} : tax;
      };
    },
  },
};
