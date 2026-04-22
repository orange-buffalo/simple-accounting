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

  const props = defineProps<{
    id?: number,
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

  const loadCustomer = props.id !== undefined ? async () => {
    const workspace = await getCustomerQuery({
      workspaceId: currentWorkspaceId,
      customerId: props.id!,
    });
    const loaded = workspace?.customer;
    if (loaded) {
      formValues.value = {
        name: loaded.name,
      };
    }
  } : undefined;

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
    if (props.id === undefined) {
      await createCustomerMutation({
        workspaceId: currentWorkspaceId,
        name: formValues.value.name,
      });
    } else {
      await editCustomerMutation({
        workspaceId: currentWorkspaceId,
        id: props.id,
        name: formValues.value.name,
      });
    }
    await navigateToCustomersOverview();
  };

  const pageHeader = computed(() => props.id !== undefined
    ? $t.editCustomer.pageHeader.edit()
    : $t.editCustomer.pageHeader.create());
</script>
