import { isNil } from 'lodash';
import { api } from '@/services/api';

export const withCategories = {
  data() {
    return {
      categories: [],
    };
  },

  async created() {
    const categoriesResponse = await api
      .pageRequest(`/workspaces/${this.$store.state.workspaces.currentWorkspace.id}/categories`)
      .eager()
      .getPageData();
    const emptyCategory = {
      name: 'Not specified', income: true, expense: true, id: null,
    };
    this.categories = [emptyCategory].concat(categoriesResponse);
  },

  computed: {
    categoryById() {
      return (categoryId) => {
        const category = this.categories.find(category => (category.id === categoryId) || (isNil(category.id) && isNil(categoryId)));
        return category || {};
      };
    },
  },
};

export default withCategories;
