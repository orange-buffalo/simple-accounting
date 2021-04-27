import { computed, ref } from '@vue/composition-api';
import { api } from '@/services/api-legacy';
import { findByIdOrEmpty } from '@/components/utils/utils';
import useCurrentWorkspace from '@/components/workspace/useCurrentWorkspace';

export default function useCategories() {
  const categories = ref([]);
  const categoriesLoaded = ref(false);

  const categoryById = computed(() => (categoryId) => findByIdOrEmpty(categories.value, categoryId));
  const { currentWorkspaceApiUrl } = useCurrentWorkspace();

  const loadCategories = async function loadCategories() {
    const categoriesResponse = await api
      .pageRequest(currentWorkspaceApiUrl('categories'))
      .eager()
      .getPageData();
    const emptyCategory = {
      name: 'Not specified',
      income: true,
      expense: true,
      id: null,
    };

    categories.value = [emptyCategory];
    Array.prototype.push.apply(categories.value, categoriesResponse);

    categoriesLoaded.value = true;
  };

  loadCategories();

  return {
    categories,
    categoryById,
    categoriesLoaded,
  };
}
