import { computed, ref } from '@vue/composition-api';
import { api } from '@/services/api-legacy';
import { findByIdOrEmpty } from '@/components/utils/utils';
import useCurrentWorkspace from '@/components/workspace/useCurrentWorkspace';

export default function useGeneralTaxes() {
  const generalTaxes = ref([]);
  const generalTaxesLoaded = ref(false);

  const generalTaxById = computed(() => (taxId) => findByIdOrEmpty(generalTaxes.value, taxId));
  const { currentWorkspaceApiUrl } = useCurrentWorkspace();

  const loadGeneralTaxes = async function loadGeneralTaxes() {
    generalTaxes.value = await api
      .pageRequest(currentWorkspaceApiUrl('general-taxes'))
      .eager()
      .getPageData();
    generalTaxesLoaded.value = true;
  };

  loadGeneralTaxes();

  return {
    generalTaxes,
    generalTaxById,
    generalTaxesLoaded,
  };
}
