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
import { useForm } from '@/components/form/use-form';
import type { CreateWorkspaceDto, EditWorkspaceDto } from '@/services/api';
import { workspacesApi } from '@/services/api';
import useNavigation from '@/services/use-navigation';
import type { PartialBy } from '@/services/utils';
import { ensureDefined } from '@/services/utils';
import { useWorkspaces } from '@/services/workspaces';

type WorkspaceForm = PartialBy<CreateWorkspaceDto, 'name' | 'defaultCurrency'>;

const props = defineProps<{
  id?: number;
}>();

const isEditing = props.id !== undefined;
// todo #459: i18n
const pageHeader = isEditing ? 'Edit Workspace' : 'Create New Workspace';

const workspaceData = ref<WorkspaceForm>({});

const loadWorkspace = async () => {
  if (isEditing) {
    // todo #462: get by id
    const response = await workspacesApi.getWorkspaces();
    const workspace = response.find((it) => it.id === props.id);
    workspaceData.value = { ...workspace };
  }
};

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

const saveWorkspace = async () => {
  if (props.id !== undefined) {
    await workspacesApi.editWorkspace({
      workspaceId: ensureDefined(props.id),
      editWorkspaceDto: workspaceData.value as EditWorkspaceDto,
    });
  } else {
    await workspacesApi.createWorkspace({
      createWorkspaceDto: workspaceData.value as CreateWorkspaceDto,
    });
  }

  await useWorkspaces().loadWorkspaces();

  await navigateToWorkspacesOverview();
};

const { formRef, submitForm } = useForm(loadWorkspace, saveWorkspace);
</script>
