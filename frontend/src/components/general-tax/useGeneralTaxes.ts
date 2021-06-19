import { computed, ref } from '@vue/composition-api';
import { findByIdOrEmpty } from '@/components/utils/utils';
import { useCurrentWorkspace } from '@/services/workspaces';
import { apiClient, consumeAllPages, GeneralTaxDto } from '@/services/api';

export default function useGeneralTaxes() {
  const generalTaxes = ref<Array<GeneralTaxDto>>([]);
  const generalTaxesLoaded = ref(false);

  const generalTaxById = computed(
    () => (taxId: number | null | undefined) => findByIdOrEmpty(generalTaxes.value, taxId),
  );
  const { currentWorkspaceId } = useCurrentWorkspace();

  const loadGeneralTaxes = async () => {
    generalTaxes.value = await consumeAllPages((pageRequest) => apiClient.getTaxes({
      workspaceId: currentWorkspaceId,
      ...pageRequest,
    }));
    generalTaxesLoaded.value = true;
  };

  // noinspection JSIgnoredPromiseFromCall
  loadGeneralTaxes();

  return {
    generalTaxes,
    generalTaxById,
    generalTaxesLoaded,
  };
}
