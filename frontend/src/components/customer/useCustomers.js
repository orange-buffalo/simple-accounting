import { computed, ref } from '@vue/composition-api';
import { api } from '@/services/api';
import { findByIdOrEmpty } from '@/components/utils/utils';
import useCurrentWorkspace from '@/components/workspace/useCurrentWorkspace';

export default function useCustomers() {
  const customers = ref([]);
  const customersLoaded = ref(false);

  const customerById = computed(() => (customerId) => findByIdOrEmpty(customers.value, customerId));
  const { currentWorkspaceApiUrl } = useCurrentWorkspace();

  const loadCustomers = async function loadCustomers() {
    customers.value = await api
      .pageRequest(currentWorkspaceApiUrl('customers'))
      .eager()
      .getPageData();
    customersLoaded.value = true;
  };

  loadCustomers();

  return {
    customers,
    customerById,
    customersLoaded,
  };
}
