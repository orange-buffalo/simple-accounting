<template>
  <div>
    <div class="sa-page-header">
      <h1>Workspaces</h1>

      <div class="sa-header-options">
        <span />

        <ElButton
          round
          @click="navigateToCreateWorkspace"
        >
          <SaIcon icon="plus-thin" />
          Create new workspace
        </ElButton>
      </div>
    </div>

    <h2>Current Workspace</h2>
    <TheWorkspacesOverviewItemPanel :workspace="currentWorkspace" />

    <template v-if="hasOtherOwnWorkspaces">
      <h2>My Other Workspaces</h2>
      <TheWorkspacesOverviewItemPanel
        v-for="workspace in ownOtherWorkspaces"
        :key="workspace.id"
        :workspace="workspace"
      />
    </template>

    <template v-if="hasSharedWorkspaces">
      <h2>Workspaces Shared With Me</h2>
      <TheWorkspacesOverviewItemPanel
        v-for="workspace in sharedWorkspaces"
        :key="`${workspace.id}-shared`"
        :workspace="workspace"
      />
    </template>
  </div>
</template>

<script lang="ts">
  import { computed, defineComponent, ref } from '@vue/composition-api';
  import SaIcon from '@/components/SaIcon';
  import useNavigation from '@/components/navigation/useNavigation';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import { apiClient, WorkspaceDto } from '@/services/api';
  import TheWorkspacesOverviewItemPanel from '@/views/settings/workspaces/TheWorkspacesOverviewItemPanel';

  export default defineComponent({
    components: {
      SaIcon,
      TheWorkspacesOverviewItemPanel,
    },

    setup() {
      const sharedWorkspaces = ref<WorkspaceDto[]>([]);
      const loadSharedWorkspaces = async () => {
        const response = await apiClient.getSharedWorkspaces();
        sharedWorkspaces.value = response.data;
      };
      loadSharedWorkspaces();

      const workspaces = ref<WorkspaceDto[]>([]);
      const loadWorkspaces = async () => {
        const response = await apiClient.getWorkspaces();
        workspaces.value = response.data;
      };
      loadWorkspaces();

      const { navigateByViewName } = useNavigation();
      const navigateToCreateWorkspace = () => navigateByViewName('create-new-workspace');

      const hasSharedWorkspaces = computed(() => sharedWorkspaces.value.length);

      const { currentWorkspace } = useCurrentWorkspace();
      const ownOtherWorkspaces = computed(() => workspaces.value.filter((it) => it.id !== currentWorkspace.id));
      const hasOtherOwnWorkspaces = computed(() => ownOtherWorkspaces.value.length);

      return {
        navigateToCreateWorkspace,
        sharedWorkspaces,
        hasSharedWorkspaces,
        ownOtherWorkspaces,
        hasOtherOwnWorkspaces,
        workspaces,
        currentWorkspace,
      };
    },
  });
</script>
