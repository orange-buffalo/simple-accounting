import { mapState } from 'vuex';
import { isNil } from 'lodash/lang';
import { api } from '@/services/api';

const withGeneralTaxes = {
  data() {
    return {
      generalTaxes: [],
    };
  },

  async created() {
    this.generalTaxes = await api.pageRequest(`/workspaces/${this.$_withGeneralTaxes_currentWorkspace.id}/general-taxes`)
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
        return isNil(tax) ? {} : tax;
      };
    },
  },
};

export default withGeneralTaxes;
