<template>
  <div>
    <div class="sa-page-header">
      <h1>{{ pageHeader }}</h1>
    </div>

    <SaForm
      ref="form"
      :loading="loading"
      :model="customer"
      :rules="customerValidationRules"
    >
      <template #default>
        <h2>General Information</h2>

        <ElFormItem
          label="Name"
          prop="name"
        >
          <ElInput
            v-model="customer.name"
            placeholder="Provide a name of the customer"
          />
        </ElFormItem>
      </template>

      <template #buttons-bar>
        <ElButton @click="navigateToCustomersOverview">
          Cancel
        </ElButton>
        <ElButton
          type="primary"
          @click="submitForm"
        >
          Save
        </ElButton>
      </template>
    </SaForm>
  </div>
</template>

<script>
  import { reactive } from '@vue/composition-api';
  import { api } from '@/services/api';
  import SaForm from '@/components/SaForm';
  import { useForm, useLoading } from '@/components/utils/utils';
  import useCurrentWorkspace from '@/components/workspace/useCurrentWorkspace';
  import useNavigation from '@/components/navigation/useNavigation';

  function useCustomerApi(customer) {
    const { currentWorkspaceApiUrl } = useCurrentWorkspace();
    const { loading, withLoading, withLoadingProducer } = useLoading();
    const { navigateByViewName } = useNavigation();

    const saveCustomer = withLoadingProducer(async () => {
      const customerToPush = {
        name: customer.name,
      };

      if (customer.id) {
        await api.put(currentWorkspaceApiUrl(`customers/${customer.id}`), customerToPush);
      } else {
        await api.post(currentWorkspaceApiUrl('customers'), customerToPush);
      }
      await navigateByViewName('customers-overview');
    });

    if (customer.id) {
      withLoading(() => api.getAndSafeAssign(currentWorkspaceApiUrl(`customers/${customer.id}`), customer));
    }

    return {
      saveCustomer,
      loading,
    };
  }

  function useCustomerForm(saveCustomer) {
    const customerValidationRules = {
      name: {
        required: true,
        message: 'Please select a name',
      },
    };

    return {
      ...useForm(saveCustomer),
      customerValidationRules,
    };
  }

  export default {
    components: {
      SaForm,
    },

    props: {
      id: {
        type: Number,
        default: null,
      },
    },

    setup({ id }) {
      const customer = reactive({ id });

      const { loading, saveCustomer } = useCustomerApi(customer);

      const { form, customerValidationRules, submitForm } = useCustomerForm(saveCustomer);

      const pageHeader = id ? 'Edit Customer' : 'Create New Customer';

      const { navigateByViewName } = useNavigation();
      const navigateToCustomersOverview = () => navigateByViewName('customers-overview');

      return {
        loading,
        submitForm,
        customer,
        customerValidationRules,
        form,
        pageHeader,
        navigateToCustomersOverview,
      };
    },
  };
</script>
