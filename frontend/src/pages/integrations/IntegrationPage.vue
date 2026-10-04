<template>
  <SaPage :header="header">
    <slot v-if="allowed && workspace" :key="workspace.id" :workspace-id="workspace.id" />
    <SaStatusLabel v-else status="failure">{{ $t.integrations.ownerOnly() }}</SaStatusLabel>
  </SaPage>
</template>

<script lang="ts" setup>
  import { computed, onUnmounted, ref } from 'vue';
  import SaPage from '@/components/SaPage.vue';
  import SaStatusLabel from '@/components/SaStatusLabel.vue';
  import { useAuth } from '@/services/api';
  import { useCurrentWorkspace, type WorkspaceInfo } from '@/services/workspaces';
  import { WORKSPACE_CHANGED_EVENT } from '@/services/events';
  import { $t } from '@/services/i18n';

  defineProps<{ header: string }>();
  const auth = useAuth();
  const regularUser = auth.isCurrentUserRegular() && !auth.isAdmin();
  const workspace = ref<WorkspaceInfo | null>(
    regularUser ? useCurrentWorkspace().currentWorkspace : null,
  );
  const allowed = computed(() => regularUser && workspace.value?.editable);
  const onWorkspaceChange = (next: WorkspaceInfo) => { workspace.value = next; };
  WORKSPACE_CHANGED_EVENT.subscribe(onWorkspaceChange);
  onUnmounted(() => WORKSPACE_CHANGED_EVENT.unsubscribe(onWorkspaceChange));
</script>
