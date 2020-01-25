import { findByIdOrEmpty } from '@/components/utils/utils';
import useGeneralTaxes from '@/components/general-tax/useGeneralTaxes';

export default {
  data() {
    return {
      generalTaxes: [],
    };
  },

  async created() {
    const { generalTaxes } = useGeneralTaxes();
    this.generalTaxes = generalTaxes;
  },

  computed: {
    generalTaxById() {
      return taxId => findByIdOrEmpty(this.generalTaxes, taxId);
    },
  },
};
