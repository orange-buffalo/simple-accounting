import { api } from '@/services/api';

export const reportGenerator = {

  props: {
    dateRange: {},
  },

  data() {
    return {
      report: null,
    };
  },

  methods: {
    async $_reportGenerator_reload() {
      const apiResponse = await this.reload(
        api,
        api.dateToString(this.dateRange[0]),
        api.dateToString(this.dateRange[1]),
      );
      this.report = apiResponse.data;
      this.$emit('report-loaded');
    },
  },

  mounted() {
    this.$_reportGenerator_reload();
  },

  watch: {
    dateRange: {
      handler() {
        this.$_reportGenerator_reload();
      },
      deep: true,
    },
  },
};

export default reportGenerator;
