import useCategories from '@/components/category/useCategories';

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
      return (categoryId) => {
        const category = this.categories
          .find(it => (it.id === categoryId) || (it.id == null && categoryId == null));
        return category || {};
      };
    },
  },
};
