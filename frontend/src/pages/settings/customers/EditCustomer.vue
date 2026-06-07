<template>
  <SaPage :header="pageHeader">

    <SaForm v-model="formValues" :on-submit="saveCustomer" :on-load="loadCustomer" :on-cancel="navigateToCustomersOverview">
      <SaFormInput
        prop="name"
        :label="$t.editCustomer.form.name.label()"
        :placeholder="$t.editCustomer.form.name.placeholder()"
      />
    </SaForm>
  </SaPage>
</template>

<script lang="ts" setup>
  import { computed, ref } from 'vue';
  import SaPage from '@/components/SaPage.vue';
  import SaForm from '@/components/form/SaForm.vue';
  import SaFormInput from '@/components/form/SaFormInput.vue';
  import useNavigation from '@/services/use-navigation';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import { graphql } from '@/services/api/gql';
  import { useMutation, useLazyQuery } from '@/services/api/use-gql-api.ts';
  import { $t } from '@/services/i18n';
  import {
    CreateCustomerMutationVariables,
    EditCustomerMutationVariables,
  } from '@/services/api/gql/graphql.ts';
  import { AsFormValues, toRequestArgs, updateFormValues } from '@/components/form/sa-form-api.ts';

  const props = defineProps<{
    id?: string,
  }>();

  const { navigateByViewName } = useNavigation();
  const navigateToCustomersOverview = async () => navigateByViewName('customers-overview');
  const { currentWorkspaceId } = useCurrentWorkspace();

  const getCustomerQuery = useLazyQuery(graphql(`
    query getCustomerForEdit($workspaceId: String!, $customerId: String!) {
      workspace(id: $workspaceId) {
        customer(id: $customerId) {
          id
          version
          name
        }
      }
    }
  `), 'workspace');

  const createCustomer = useMutation(graphql(`
    mutation createCustomer(
      $workspaceId: String!,
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

  const editCustomer = useMutation(graphql(`
    mutation editCustomer(
      $workspaceId: String!,
      $id: String!,
      $version: Int!,
      $name: String!
    ) {
      editCustomer(
        workspaceId: $workspaceId,
        id: $id,
        version: $version,
        name: $name
      ) {
        id
      }
    }
  `), 'editCustomer');

  type CustomerFormValues = AsFormValues<[CreateCustomerMutationVariables, EditCustomerMutationVariables]>;

  const formValues = ref<CustomerFormValues>({
    workspaceId: currentWorkspaceId,
    id: props.id,
    name: '',
  });

  const loadCustomer = props.id !== undefined ? async () => {
    const workspace = await getCustomerQuery({
      workspaceId: currentWorkspaceId,
      customerId: props.id!,
    });
    updateFormValues(formValues, workspace.customer);
  } : undefined;

  const saveCustomer = async () => {
    if (props.id === undefined) {
      await createCustomer(toRequestArgs(formValues));
    } else {
      await editCustomer(toRequestArgs(formValues));
    }
    await navigateToCustomersOverview();
  };

  const pageHeader = computed(() => props.id !== undefined
    ? $t.value.editCustomer.pageHeader.edit()
    : $t.value.editCustomer.pageHeader.create());
</script>
