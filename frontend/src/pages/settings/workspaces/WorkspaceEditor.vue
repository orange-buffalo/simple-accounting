<template>
  <div>
    <div class="sa-page-header">
      <h1>{{ pageHeader }}</h1>
    </div>

    <SaLegacyForm
      ref="formRef"
      :model="workspaceData"
      :rules="workspaceValidationRules"
    >
      <template #default>
        <div class="row">
          <div class="col col-xs-12 col-lg-6">
            <h2>General Information</h2>
            <ElFormItem
              label="Workspace Name"
              prop="name"
            >
              <ElInput v-model="workspaceData.name" />
            </ElFormItem>

            <ElFormItem
              label="Default Currency"
              prop="defaultCurrency"
            >
              <SaCurrencyInput
                v-model="workspaceData.defaultCurrency"
                :disabled="isEditing"
              />
            </ElFormItem>
          </div>
        </div>
      </template>

      <template #buttons-bar>
        <ElButton @click="navigateToWorkspacesOverview">
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
  import SaCurrencyInput from '@/components/currency-input/SaCurrencyInput.vue';
  import SaLegacyForm from '@/components/form/SaLegacyForm.vue';
  import { useWorkspaces } from '@/services/workspaces';
  import useNavigation from '@/services/use-navigation';
  import { useForm } from '@/components/form/use-form';
  import { graphql } from '@/services/api/gql';
  import { useLazyQuery, useMutation } from '@/services/api/use-gql-api';

  interface WorkspaceForm {
    name?: string;
    defaultCurrency?: string;
  }

  const props = defineProps<{
    id?: number,
  }>();

  const isEditing = props.id !== undefined;
  // todo #459: i18n
  const pageHeader = isEditing ? 'Edit Workspace' : 'Create New Workspace';

  const workspaceData = ref<WorkspaceForm>({});

  const workspaceQuery = graphql(`
    query workspaceForEditor($id: Long!) {
      workspace(id: $id) {
        id
        name
        defaultCurrency
      }
    }
  `);

  const createWorkspaceMutation = graphql(`
    mutation createWorkspaceEditor($name: String!, $defaultCurrency: String!) {
      createWorkspace(name: $name, defaultCurrency: $defaultCurrency) {
        id
        name
        defaultCurrency
      }
    }
  `);

  const editWorkspaceMutation = graphql(`
    mutation editWorkspaceEditor($id: Long!, $name: String!) {
      editWorkspace(id: $id, name: $name) {
        id
        name
        defaultCurrency
      }
    }
  `);

  const loadWorkspace = useLazyQuery(workspaceQuery, 'workspace');
  const executeCreate = useMutation(createWorkspaceMutation, 'createWorkspace');
  const executeEdit = useMutation(editWorkspaceMutation, 'editWorkspace');

  const { navigateByViewName } = useNavigation();
  const navigateToWorkspacesOverview = () => navigateByViewName('workspaces-overview');

  // todo #459: i18n
  const workspaceValidationRules = {
    name: [
      {
        required: true,
        message: 'Please provide the name',
      },
      {
        max: 255,
        message: 'Name is too long',
      },
    ],
    defaultCurrency: {
      required: true,
      message: 'Please select a default currency',
    },
  };

  const initForm = async () => {
    if (isEditing) {
      const workspace = await loadWorkspace({ id: props.id! });
      workspaceData.value = { name: workspace.name, defaultCurrency: workspace.defaultCurrency };
    }
  };

  const saveWorkspace = async () => {
    if (props.id !== undefined) {
      await executeEdit({ id: props.id, name: workspaceData.value.name! });
    } else {
      await executeCreate({
        name: workspaceData.value.name!,
        defaultCurrency: workspaceData.value.defaultCurrency!,
      });
    }

    await useWorkspaces().loadWorkspaces();
    await navigateToWorkspacesOverview();
  };

  const {
    formRef,
    submitForm,
  } = useForm(initForm, saveWorkspace);
</script>
