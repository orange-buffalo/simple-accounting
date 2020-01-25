import { computed, reactive, ref } from '@vue/composition-api';
import { api } from '@/services/api';
import { app } from '@/services/app-services';
import { findByIdOrEmpty } from '@/components/utils/utils';

export default function useCategories() {
  const categories = reactive([]);
  const categoriesLoaded = ref(false);

  const categoryById = computed(() => categoryId => findByIdOrEmpty(categories, categoryId));

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
    // Array.prototype.push.apply is not reactive
    categoriesResponse.forEach(it => categories.push(it));
    categoriesLoaded.value = true;
  };

  loadCategories();

  return {
    categories,
    categoryById,
    categoriesLoaded,
  };
}
