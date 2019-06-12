import {isNil} from 'lodash'
import {api} from '@/services/api'

export const withCategories = {
  data: function () {
    return {
      categories: []
    }
  },

  created: async function () {
    let categoriesResponse = await api
        .pageRequest(`/user/workspaces/${this.$store.state.workspaces.currentWorkspace.id}/categories`)
        .eager()
        .getPageData()
    let emptyCategory = {name: "Not specified", income: true, expense: true, id: null}
    this.categories = [emptyCategory].concat(categoriesResponse)
  },

  computed: {
    categoryById: function () {
      return categoryId => {
        let category = this.categories.find(category =>
            (category.id === categoryId) || (isNil(category.id) && isNil(categoryId)))
        return category ? category : {}
      }
    }
  }
}

export default withCategories