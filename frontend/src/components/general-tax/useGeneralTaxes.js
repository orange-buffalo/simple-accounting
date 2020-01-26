import { computed, reactive, ref } from '@vue/composition-api';
import { api } from '@/services/api';
import { findByIdOrEmpty } from '@/components/utils/utils';
import useCurrentWorkspace from '@/components/workspace/useCurrentWorkspace';

export default function useGeneralTaxes() {
  const generalTaxes = reactive([]);
  const generalTaxesLoaded = ref(false);

  const generalTaxById = computed(() => taxId => findByIdOrEmpty(generalTaxes, taxId));
  const { currentWorkspaceApiUrl } = useCurrentWorkspace();

  const loadGeneralTaxes = async function loadGeneralTaxes() {
    const taxesResponse = await api
      .pageRequest(currentWorkspaceApiUrl('general-taxes'))
      .eager()
      .getPageData();
    // Array.prototype.push.apply is not reactive
    taxesResponse.forEach(it => generalTaxes.push(it));
    generalTaxesLoaded.value = true;
  };

  loadGeneralTaxes();

  return {
    generalTaxes,
    generalTaxById,
    generalTaxesLoaded,
  };
}
