import { mapState } from 'vuex';
import { api } from '@/services/api';
import { findByIdOrEmpty } from '@/components/utils/utils';

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
      return taxId => findByIdOrEmpty(this.generalTaxes, taxId);
    },
  },
};
