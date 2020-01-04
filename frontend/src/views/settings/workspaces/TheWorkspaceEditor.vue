<template>
  <div>
    <div class="sa-page-header">
      <h1>{{ pageHeader }}</h1>
    </div>

    <SaForm
      ref="workspaceForm"
      :model="workspaceForm"
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
              <ElInput v-model="workspaceForm.name" />
            </ElFormItem>

            <ElFormItem
              label="Default Currency"
              prop="defaultCurrency"
            >
              <SaCurrencyInput
                v-model="workspaceForm.defaultCurrency"
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

<script>
  import { api } from '@/services/api';
  import SaCurrencyInput from '@/components/SaCurrencyInput';
  import SaForm from '@/components/SaForm';
  import withWorkspaces from '@/components/mixins/with-workspaces';

  export default {
    name: 'TheWorkspaceEditor',

    components: {
      SaCurrencyInput,
      SaForm,
    },

    mixins: [withWorkspaces],

    data() {
      return {
        workspaceForm: {},
        workspaceValidationRules: {
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
        },
      };
    },

    computed: {
      isEditing() {
        return this.$route.params.id != null;
      },

      pageHeader() {
        return this.isEditing ? 'Edit Workspace' : 'Create New Workspace';
      },
    },

    async created() {
      if (this.isEditing) {
        const workspace = this.workspaces.find(it => it.id === this.$route.params.id);
        this.workspaceForm = { ...workspace };
      } else {
        this.workspaceForm = {};
      }
    },

    methods: {
      navigateToWorkspacesOverview() {
        this.$router.push({ name: 'workspaces-overview' });
      },

      async saveWorkspace() {
        try {
          await this.$refs.workspaceForm.validate();
        } catch (e) {
          return;
        }

        if (this.workspaceForm.id) {
          await api.put(`/workspaces/${this.workspaceForm.id}`, {
            name: this.workspaceForm.name,
          });
        } else {
          await api.post('/workspaces', {
            name: this.workspaceForm.name,
            defaultCurrency: this.workspaceForm.defaultCurrency,
          });
        }

        // todo #90: when categories are removed from workspace, just use the reply to update current workspace
        await this.$store.dispatch('workspaces/loadWorkspaces');

        this.navigateToWorkspacesOverview();
      },
    },
  };
</script>
