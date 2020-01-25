import useCategories from '@/components/category/useCategories';
import { findByIdOrEmpty } from '@/components/utils/utils';

export default {
  data() {
    return {
      categories: [],
    };
  },

  async created() {
    const { categories } = useCategories();
    this.categories = categories;
  },

  computed: {
    categoryById() {
      return categoryId => findByIdOrEmpty(this.categories, categoryId);
    },
  },
};
