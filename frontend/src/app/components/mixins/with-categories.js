import {mapGetters} from 'vuex'

export const withCategories = {
  computed: {
    ...mapGetters({
      $_withCategories_categoryById: 'workspaces/categoryById'
    }),

    categoryById: function () {
      return categoryId => {
        let category = this.$_withCategories_categoryById(categoryId)
        return category ? category : {}
      }
    }
  }
}

export default withCategories