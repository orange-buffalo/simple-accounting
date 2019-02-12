import {mapState} from 'vuex'
import {isNil} from 'lodash'
import {api} from '@/services/api'

export const withTaxes = {
  data: function () {
    return {
      taxes: []
    }
  },

  created: async function () {
    this.taxes = await api.pageRequest(`/user/workspaces/${this.$_withTaxes_currentWorkspace.id}/taxes`)
        .eager()
        .getPageData()
  },

  computed: {
    ...mapState({
      $_withTaxes_currentWorkspace: state => state.workspaces.currentWorkspace
    }),

    taxById: function () {
      return taxId => {
        let tax = this.taxes.find(tax => tax.id === taxId)
        return isNil(tax) ? {} : tax
      }
    }
  }
}

export default withTaxes