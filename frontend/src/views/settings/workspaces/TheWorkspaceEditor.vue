<template>
  <div>
    <div class="sa-page-header">
      <h1>{{ pageHeader }}</h1>
    </div>

    <SaForm
      ref="workspaceForm"
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
          @click="saveWorkspace"
        >
          Save
        </ElButton>
      </template>
    </SaForm>
  </div>
</template>

<script lang="ts">
  import SaCurrencyInput from '@/components/SaCurrencyInput';
  import SaForm from '@/components/SaForm';
  import { useWorkspaces } from '@/services/workspaces';
  import { defineComponent, PropType, ref } from '@vue/composition-api';
  import { apiClient } from '@/services/api';
  import useNavigation from '@/components/navigation/useNavigation';
  import { ElForm } from 'element-ui/types/form';

  interface WorkspaceForm {
    name?: string;
    defaultCurrency?: string;
  }

  export default defineComponent({
    components: {
      SaCurrencyInput,
      SaForm,
    },

    props: {
      id: {
        type: Number as PropType<number | null>,
        default: null,
      },
    },

    setup(props) {
      const isEditing = props.id != null;
      // todo i18n
      const pageHeader = isEditing ? 'Edit Workspace' : 'Create New Workspace';

      const workspaceData = ref<WorkspaceForm>({});

      const loadWorkspace = async () => {
        if (isEditing) {
          // todo get by id
          const response = await apiClient.getWorkspaces();
          const workspace = response.data.find((it) => it.id === props.id);
          workspaceData.value = { ...workspace };
        }
      };
      loadWorkspace();

      const { navigateByViewName } = useNavigation();
      const navigateToWorkspacesOverview = () => navigateByViewName('workspaces-overview');

      const workspaceForm = ref<ElForm | null>(null);
      // todo i18n
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
        try {
          await workspaceForm.value!.validate();
        } catch (e) {
          return;
        }

        if (props.id) {
          await apiClient.editWorkspace({
            workspaceId: props.id!,
          }, {
            name: workspaceData.value.name!,
          });
        } else {
          await apiClient.createWorkspace({}, {
            defaultCurrency: workspaceData.value.defaultCurrency!,
            name: workspaceData.value.name!,
          });
        }

        await useWorkspaces()
          .loadWorkspaces();

        await navigateToWorkspacesOverview();
      };

      return {
        isEditing,
        pageHeader,
        navigateToWorkspacesOverview,
        workspaceData,
        workspaceForm,
        workspaceValidationRules,
        saveWorkspace,
      };
    },
  });
</script>
