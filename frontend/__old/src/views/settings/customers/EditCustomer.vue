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
  import SaForm from '@/components/SaForm';
  import { useForm, useLoading } from '@/components/utils/utils';
  import useNavigation from '@/components/navigation/useNavigation';
  import { useApiCrud } from '@/components/utils/api-utils';

  function useCustomerApi(customer) {
    const {
      loadEntity,
      loading,
      saveEntity,
    } = useApiCrud({
      apiEntityPath: 'customers',
      entity: customer,
      ...useLoading(),
    });

    const saveCustomer = async () => {
      await saveEntity(customer);
      await navigateToCustomersOverview();
    };

    loadEntity();

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

  function navigateToCustomersOverview() {
    const { navigateByViewName } = useNavigation();
    navigateByViewName('customers-overview');
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

    setup(props) {
      const customer = reactive({ id: props.id });

      const {
        loading,
        saveCustomer,
      } = useCustomerApi(customer);

      const {
        form,
        customerValidationRules,
        submitForm,
      } = useCustomerForm(saveCustomer);

      const pageHeader = props.id ? 'Edit Customer' : 'Create New Customer';

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
