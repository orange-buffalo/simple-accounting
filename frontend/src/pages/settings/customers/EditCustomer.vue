<template>
  <div>
    <div class="sa-page-header">
      <h1>Edit Customer</h1>
    </div>

    <SaForm v-model="formValues" :on-submit="saveCustomer" :on-load="loadCustomer" :on-cancel="navigateToCustomersOverview">
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
  import { useMutation, useLazyQuery } from '@/services/api/use-gql-api.ts';

  const props = defineProps<{
    id: number,
  }>();

  const { navigateByViewName } = useNavigation();
  const navigateToCustomersOverview = async () => navigateByViewName('customers-overview');

  type CustomerFormValues = {
    name: string,
  };

  const formValues = ref<CustomerFormValues>({
    name: '',
  });

  const { currentWorkspaceId } = useCurrentWorkspace();

  const getCustomerQuery = useLazyQuery(graphql(`
    query getCustomerForEdit($workspaceId: Long!, $customerId: Long!) {
      workspace(id: $workspaceId) {
        customer(id: $customerId) {
          id
          name
        }
      }
    }
  `), 'workspace');

  const loadCustomer = async () => {
    const workspace = await getCustomerQuery({
      workspaceId: currentWorkspaceId,
      customerId: props.id,
    });
    const loaded = workspace?.customer;
    if (loaded) {
      formValues.value = {
        name: loaded.name,
      };
    }
  };

  const editCustomerMutation = useMutation(graphql(`
    mutation editCustomerMutation(
      $workspaceId: Long!,
      $id: Long!,
      $name: String!
    ) {
      editCustomer(
        workspaceId: $workspaceId,
        id: $id,
        name: $name
      ) {
        id
      }
    }
  `), 'editCustomer');

  const saveCustomer = async () => {
    await editCustomerMutation({
      workspaceId: currentWorkspaceId,
      id: props.id,
      name: formValues.value.name,
    });
    await navigateToCustomersOverview();
  };
</script>
