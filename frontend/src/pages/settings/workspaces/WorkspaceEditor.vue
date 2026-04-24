<template>
  <div>
    <div class="sa-page-header">
      <h1>{{ pageHeader }}</h1>
    </div>

    <SaForm v-model="formValues" :on-submit="saveWorkspace" :on-load="loadWorkspaceData" :on-cancel="navigateToWorkspacesOverview">
      <h2>{{ $t.workspaceEditor.generalInformation.header() }}</h2>
      <SaFormInput prop="name" :label="$t.workspaceEditor.generalInformation.workspaceName.label()" />
      <SaFormCurrencyInput prop="defaultCurrency" :label="$t.workspaceEditor.generalInformation.defaultCurrency.label()" :disabled="isEditing" />
    </SaForm>
  </div>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import { $t } from '@/services/i18n';
  import SaForm from '@/components/form/SaForm.vue';
  import SaFormInput from '@/components/form/SaFormInput.vue';
  import SaFormCurrencyInput from '@/components/form/SaFormCurrencyInput.vue';
  import { useWorkspaces } from '@/services/workspaces';
  import useNavigation from '@/services/use-navigation';
  import { graphql } from '@/services/api/gql';
  import { useLazyQuery, useMutation } from '@/services/api/use-gql-api';

  type WorkspaceForm = {
    name: string,
    defaultCurrency: string,
  };

  const props = defineProps<{
    id?: number,
  }>();

  const isEditing = props.id !== undefined;
  const pageHeader = isEditing
    ? $t.value.workspaceEditor.pageHeader.edit()
    : $t.value.workspaceEditor.pageHeader.create();

  const formValues = ref<WorkspaceForm>({
    name: '',
    defaultCurrency: '',
  });

  const loadWorkspaceQuery = useLazyQuery(graphql(`
    query workspaceForEditor($id: Long!) {
      workspace(id: $id) {
        id
        name
        defaultCurrency
      }
    }
  `), 'workspace');

  const createWorkspaceMutation = useMutation(graphql(`
    mutation createWorkspaceEditor($name: String!, $defaultCurrency: String!) {
      createWorkspace(name: $name, defaultCurrency: $defaultCurrency) {
        id
      }
    }
  `), 'createWorkspace');

  const editWorkspaceMutation = useMutation(graphql(`
    mutation editWorkspaceEditor($id: Long!, $name: String!) {
      editWorkspace(id: $id, name: $name) {
        id
      }
    }
  `), 'editWorkspace');

  const { navigateByViewName } = useNavigation();
  const navigateToWorkspacesOverview = async () => navigateByViewName('workspaces-overview');

  const loadWorkspaceData = isEditing ? async () => {
    const workspace = await loadWorkspaceQuery({ id: props.id! });
    if (workspace) {
      formValues.value = { name: workspace.name, defaultCurrency: workspace.defaultCurrency };
    }
  } : undefined;

  const saveWorkspace = async () => {
    if (props.id !== undefined) {
      await editWorkspaceMutation({ id: props.id, name: formValues.value.name });
    } else {
      await createWorkspaceMutation({
        name: formValues.value.name,
        defaultCurrency: formValues.value.defaultCurrency,
      });
    }
    await useWorkspaces().loadWorkspaces();
    await navigateToWorkspacesOverview();
  };
</script>
