<template>
  <div>
    <div class="sa-page-header">
      <h1>Create New Customer</h1>
    </div>

    <SaLegacyForm
      ref="formRef"
      :model="customer"
      :rules="customerValidationRules"
      :initially-loading="false"
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
    </SaLegacyForm>
  </div>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import SaLegacyForm from '@/components/form/SaLegacyForm.vue';
  import useNavigation from '@/services/use-navigation';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import { useForm } from '@/components/form/use-form';
  import { graphql } from '@/services/api/gql';
  import { useMutation } from '@/services/api/use-gql-api.ts';

  type CustomerFormValues = {
    name?: string,
  };
  const customer = ref<CustomerFormValues>({});

  const customerValidationRules = {
    name: {
      required: true,
      message: 'Please provide a name',
    },
  };

  const { navigateByViewName } = useNavigation();
  const navigateToCustomersOverview = async () => navigateByViewName('customers-overview');

  const { currentWorkspaceId } = useCurrentWorkspace();

  const createCustomerMutation = useMutation(graphql(`
    mutation createCustomerMutation(
      $workspaceId: Long!,
      $name: String!
    ) {
      createCustomer(
        workspaceId: $workspaceId,
        name: $name
      ) {
        id
      }
    }
  `), 'createCustomer');

  const saveCustomer = async () => {
    await createCustomerMutation({
      workspaceId: currentWorkspaceId,
      name: customer.value.name!,
    });
    await navigateToCustomersOverview();
  };

  const {
    formRef,
    submitForm,
  } = useForm(async () => {
    // no op
  }, saveCustomer);
</script>
