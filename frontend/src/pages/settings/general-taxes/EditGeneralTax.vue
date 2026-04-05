<template>
  <div>
    <div class="sa-page-header">
      <h1>{{ pageHeader }}</h1>
    </div>

    <SaLegacyForm
      ref="formRef"
      :model="tax"
      :rules="taxValidationRules"
    >
      <template #default>
        <h2>General Information</h2>

        <ElFormItem
          label="Title"
          prop="title"
        >
          <ElInput
            v-model="tax.title"
            placeholder="Provide a title of the tax"
          />
        </ElFormItem>

        <ElFormItem
          label="Description"
          prop="description"
        >
          <ElInput
            v-model="tax.description"
            placeholder="Short description of a tax"
          />
        </ElFormItem>

        <!--todo #79: input in bps-->
        <ElFormItem
          label="Rate"
          prop="rateInBps"
        >
          <ElInput
            v-model="tax.rateInBps"
            placeholder="Provide a rate for this tax"
          />
        </ElFormItem>
      </template>

      <template #buttons-bar>
        <ElButton @click="navigateToTaxesOverview">
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
  import { useMutation, useLazyQuery } from '@/services/api/use-gql-api.ts';

  const props = defineProps<{
    id?: number,
  }>();

  const { navigateByViewName } = useNavigation();
  const navigateToTaxesOverview = () => navigateByViewName('general-taxes-overview');

  type TaxFormValues = {
    title?: string,
    description?: string,
    rateInBps?: number,
  };
  const tax = ref<TaxFormValues>({});

  const { currentWorkspaceId } = useCurrentWorkspace();

  const getGeneralTaxQuery = useLazyQuery(graphql(`
    query getGeneralTaxForEdit($workspaceId: Long!, $taxId: Long!) {
      workspace(id: $workspaceId) {
        generalTax(id: $taxId) {
          id
          title
          description
          rateInBps
        }
      }
    }
  `), 'workspace');

  const loadTax = async () => {
    if (props.id !== undefined) {
      const workspace = await getGeneralTaxQuery({
        workspaceId: currentWorkspaceId,
        taxId: props.id,
      });
      const loaded = workspace?.generalTax;
      if (loaded) {
        tax.value = {
          title: loaded.title,
          description: loaded.description ?? undefined,
          rateInBps: loaded.rateInBps,
        };
      }
    }
  };

  const createGeneralTaxMutation = useMutation(graphql(`
    mutation createGeneralTaxMutation(
      $workspaceId: Long!,
      $title: String!,
      $description: String,
      $rateInBps: Int!
    ) {
      createGeneralTax(
        workspaceId: $workspaceId,
        title: $title,
        description: $description,
        rateInBps: $rateInBps
      ) {
        id
      }
    }
  `), 'createGeneralTax');

  const editGeneralTaxMutation = useMutation(graphql(`
    mutation editGeneralTaxMutation(
      $workspaceId: Long!,
      $id: Long!,
      $title: String!,
      $description: String,
      $rateInBps: Int!
    ) {
      editGeneralTax(
        workspaceId: $workspaceId,
        id: $id,
        title: $title,
        description: $description,
        rateInBps: $rateInBps
      ) {
        id
      }
    }
  `), 'editGeneralTax');

  const saveTax = async () => {
    if (props.id === undefined) {
      await createGeneralTaxMutation({
        workspaceId: currentWorkspaceId,
        title: tax.value.title!,
        description: tax.value.description ?? null,
        rateInBps: Number(tax.value.rateInBps!),
      });
    } else {
      await editGeneralTaxMutation({
        workspaceId: currentWorkspaceId,
        id: props.id,
        title: tax.value.title!,
        description: tax.value.description ?? null,
        rateInBps: Number(tax.value.rateInBps!),
      });
    }
    await navigateToTaxesOverview();
  };

  const taxValidationRules = {
    title: {
      required: true,
      message: 'Please provide a title',
    },
    rateInBps: {
      required: true,
      message: 'Please provide the rate',
    },
  };

  const {
    formRef,
    submitForm,
  } = useForm(loadTax, saveTax);

  const pageHeader = props.id ? 'Edit General Tax' : 'Create New General Tax';
</script>
