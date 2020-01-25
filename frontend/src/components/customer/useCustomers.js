import { computed, reactive, ref } from '@vue/composition-api';
import { api } from '@/services/api';
import { app } from '@/services/app-services';
import { findByIdOrEmpty } from '@/components/utils/utils';

export default function useCustomers() {
  const customers = reactive([]);
  const customersLoaded = ref(false);

  const customerById = computed(() => customerId => findByIdOrEmpty(customers, customerId));

  const loadCustomers = async function loadCustomers() {
    const customersResponse = await api
      .pageRequest(`/workspaces/${app.store.state.workspaces.currentWorkspace.id}/customers`)
      .eager()
      .getPageData();
    // Array.prototype.push.apply is not reactive
    customersResponse.forEach(it => customers.push(it));
    customersLoaded.value = true;
  };

  loadCustomers();

  return {
    customers,
    customerById,
    customersLoaded,
  };
}
