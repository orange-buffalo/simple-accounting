import { computed, ref } from '@vue/composition-api';
import { findByIdOrEmpty } from '@/components/utils/utils';
import { useCurrentWorkspace } from '@/services/workspaces';
import { apiClient, consumeAllPages, CustomerDto } from '@/services/api';

export default function useCustomers() {
  const customers = ref<CustomerDto[]>([]);
  const customersLoaded = ref(false);

  const customerById = computed(() => (customerId: number) => findByIdOrEmpty(customers.value, customerId));
  const { currentWorkspaceId } = useCurrentWorkspace();

  const loadCustomers = async function loadCustomers() {
    customers.value = await consumeAllPages((pageRequest) => apiClient.getCustomers({
      workspaceId: currentWorkspaceId,
      ...pageRequest,
    }));

    customersLoaded.value = true;
  };

  loadCustomers();

  return {
    customers,
    customerById,
    customersLoaded,
  };
}
