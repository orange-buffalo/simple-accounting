<template>
  <div>
    <div class="sa-page-header">
      <h1>Create New Customer</h1>
    </div>

    <SaForm v-model="formValues" :on-submit="saveCustomer" :on-cancel="navigateToCustomersOverview">
      <SaFormInput prop="name" label="Name" placeholder="Provide a name of the customer" />
    </SaForm>
  </div>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import SaForm from '@/components/form/SaForm.vue';
  import SaFormInput from '@/components/form/SaFormInput.vue';
  import useNavigation from '@/services/use-navigation';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import { graphql } from '@/services/api/gql';
  import { useMutation } from '@/services/api/use-gql-api.ts';

  type CustomerFormValues = {
    name: string,
  };

  const formValues = ref<CustomerFormValues>({
    name: '',
  });

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
      name: formValues.value.name,
    });
    await navigateToCustomersOverview();
  };
</script>
