<template>
  <div>
    <div class="sa-page-header">
      <h1>{{ pageHeader }}</h1>
    </div>

    <SaForm v-model="formValues" :on-submit="saveCustomer" :on-load="loadCustomer" :on-cancel="navigateToCustomersOverview">
      <SaFormInput
        prop="name"
        :label="$t.editCustomer.form.name.label()"
        :placeholder="$t.editCustomer.form.name.placeholder()"
      />
    </SaForm>
  </div>
</template>

<script lang="ts" setup>
  import { computed, ref } from 'vue';
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
    id?: number,
  }>();

  const { navigateByViewName } = useNavigation();
  const navigateToCustomersOverview = async () => navigateByViewName('customers-overview');
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

  const createCustomer = useMutation(graphql(`
    mutation createCustomer(
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

  const editCustomer = useMutation(graphql(`
    mutation editCustomer(
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

  type CustomerFormValues = AsFormValues<[CreateCustomerMutationVariables, EditCustomerMutationVariables]>;

  const formValues = ref<CustomerFormValues>({
    workspaceId: currentWorkspaceId,
    id: props.id,
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
