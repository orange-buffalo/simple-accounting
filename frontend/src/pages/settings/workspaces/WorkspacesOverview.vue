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
    <WorkspacesOverviewItemPanel :workspace="currentWorkspace" />

    <template v-if="hasOtherOwnWorkspaces">
      <h2>My Other Workspaces</h2>
      <WorkspacesOverviewItemPanel
        v-for="workspace in ownOtherWorkspaces"
        :key="workspace.id"
        :workspace="workspace"
      />
    </template>

    <template v-if="hasSharedWorkspaces">
      <h2>Workspaces Shared With Me</h2>
      <WorkspacesOverviewItemPanel
        v-for="workspace in sharedWorkspaces"
        :key="`${workspace.id}-shared`"
        :workspace="workspace"
      />
    </template>
  </div>
</template>

<script lang="ts" setup>
import { computed, ref } from 'vue';
import SaIcon from '@/components/SaIcon.vue';
import WorkspacesOverviewItemPanel from '@/pages/settings/workspaces/WorkspacesOverviewItemPanel.vue';
import type { WorkspaceDto } from '@/services/api';
import { workspacesApi } from '@/services/api';
import useNavigation from '@/services/use-navigation';
import { useCurrentWorkspace } from '@/services/workspaces';

const sharedWorkspaces = ref<WorkspaceDto[]>([]);
const loadSharedWorkspaces = async () => {
  sharedWorkspaces.value = await workspacesApi.getSharedWorkspaces();
};
loadSharedWorkspaces();

const workspaces = ref<WorkspaceDto[]>([]);
const loadWorkspaces = async () => {
  workspaces.value = await workspacesApi.getWorkspaces();
};
loadWorkspaces();

const { navigateByViewName } = useNavigation();
const navigateToCreateWorkspace = () => navigateByViewName('create-new-workspace');

const hasSharedWorkspaces = computed(() => sharedWorkspaces.value.length);

const { currentWorkspace } = useCurrentWorkspace();
const ownOtherWorkspaces = computed(() => workspaces.value.filter((it) => it.id !== currentWorkspace.id));
const hasOtherOwnWorkspaces = computed(() => ownOtherWorkspaces.value.length);
</script>
