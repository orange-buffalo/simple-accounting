<template>
  <SaPage :header="header">
    <slot :key="workspace.id" :workspace-id="workspace.id" />
  </SaPage>
</template>

<script lang="ts" setup>
  import { onUnmounted, ref } from 'vue';
  import SaPage from '@/components/SaPage.vue';
  import { useCurrentWorkspace, type WorkspaceInfo } from '@/services/workspaces';
  import { WORKSPACE_CHANGED_EVENT } from '@/services/events';

  defineProps<{ header: string }>();
  const workspace = ref<WorkspaceInfo>(useCurrentWorkspace().currentWorkspace);
  const onWorkspaceChange = (next: WorkspaceInfo) => { workspace.value = next; };
  WORKSPACE_CHANGED_EVENT.subscribe(onWorkspaceChange);
  onUnmounted(() => WORKSPACE_CHANGED_EVENT.unsubscribe(onWorkspaceChange));
</script>
