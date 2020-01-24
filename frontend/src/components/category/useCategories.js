import { computed, reactive, ref } from '@vue/composition-api';
import { api } from '@/services/api';
import { app } from '@/services/app-services';

export default function useCategories() {
  const categories = reactive([]);
  const categoriesLoaded = ref(false);

  const categoryById = computed(() => (categoryId) => {
    const category = categories
      .find(it => (it.id === categoryId) || (it.id == null && categoryId == null));
    return category || {};
  });

  const loadCategories = async function loadCategories() {
    const categoriesResponse = await api
      .pageRequest(`/workspaces/${app.store.state.workspaces.currentWorkspace.id}/categories`)
      .eager()
      .getPageData();
    const emptyCategory = {
      name: 'Not specified',
      income: true,
      expense: true,
      id: null,
    };
    categories.push(emptyCategory);
    Array.prototype.push.apply(categories, categoriesResponse);
    categoriesLoaded.value = true;
  };

  loadCategories();

  return {
    categories,
    categoryById,
    categoriesLoaded,
  };
}
