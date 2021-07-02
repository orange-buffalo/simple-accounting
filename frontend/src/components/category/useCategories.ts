import { computed, ref } from '@vue/composition-api';
import { findByIdOrEmpty } from '@/components/utils/utils';
import { useCurrentWorkspace } from '@/services/workspaces';
import { apiClient, consumeAllPages } from '@/services/api';
import { CategoryDto } from '@/services/api/api-types';

export default function useCategories() {
  const categories = ref<CategoryDto[]>([]);
  const categoriesLoaded = ref(false);

  const categoryById = computed(() => {
    const currentCategories = categories.value;
    return (categoryId?: number) => findByIdOrEmpty(currentCategories, categoryId);
  });
  const { currentWorkspaceId } = useCurrentWorkspace();

  const loadCategories = async () => {
    const categoriesResponse: CategoryDto[] = await consumeAllPages((pageRequest) => apiClient.getCategories({
      workspaceId: currentWorkspaceId,
      ...pageRequest,
    }));

    // todo i18n
    const emptyCategory = {
      name: 'Not specified',
      income: true,
      expense: true,
      id: null,
      version: -1,
      description: null,
    };

    categories.value = [emptyCategory];
    Array.prototype.push.apply(categories.value, categoriesResponse);

    categoriesLoaded.value = true;
  };

  // noinspection JSIgnoredPromiseFromCall
  loadCategories();

  return {
    categories,
    categoryById,
    categoriesLoaded,
  };
}
