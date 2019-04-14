import {api} from '@/services/api'

export const reportGenerator = {

  props: {
    dateRange: {}
  },

  data: function () {
    return {
      report: null
    }
  },

  methods: {
    $_reportGenerator_reload: async function () {
      let apiResponse = await this.reload(
          api,
          api.dateToString(this.dateRange[0]),
          api.dateToString(this.dateRange[1]))
      this.report = apiResponse.data
      this.$emit('report-loaded')
    }
  },

  mounted: function () {
    this.$_reportGenerator_reload()
  },

  watch: {
    dateRange: {
      handler: function () {
        this.$_reportGenerator_reload()
      },
      deep: true
    }
  }
}

export default reportGenerator