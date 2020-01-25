import { computed, reactive, ref } from '@vue/composition-api';
import { api } from '@/services/api';
import { app } from '@/services/app-services';
import { findByIdOrEmpty } from '@/components/utils/utils';

export default function useGeneralTaxes() {
  const generalTaxes = reactive([]);
  const generalTaxesLoaded = ref(false);

  const generalTaxById = computed(() => taxId => findByIdOrEmpty(generalTaxes, taxId));

  const loadGeneralTaxes = async function loadGeneralTaxes() {
    const taxesResponse = await api
      .pageRequest(`/workspaces/${app.store.state.workspaces.currentWorkspace.id}/general-taxes`)
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
