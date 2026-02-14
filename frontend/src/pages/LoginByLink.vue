<template>
  <div />
</template>

<script lang="ts" setup>
import { onMounted } from 'vue';
import { useAuth, workspacesApi } from '@/services/api';
import useNavigation from '@/services/use-navigation';
import { initWorkspace, useWorkspaces } from '@/services/workspaces';

const props = defineProps<{
  token?: string;
}>();

const { navigateByPath } = useNavigation();

onMounted(async () => {
  if (props.token === undefined) {
    await navigateByPath('/');
    return;
  }
  const { isLoggedIn, loginBySharedToken } = useAuth();

  try {
    if (isLoggedIn()) {
      const workspace = await workspacesApi.saveSharedWorkspace({
        saveSharedWorkspaceRequestDto: {
          token: props.token,
        },
      });

      const { loadWorkspaces, setCurrentWorkspace } = useWorkspaces();
      setCurrentWorkspace(workspace);
      await loadWorkspaces();
      await navigateByPath('/');
    } else if (await loginBySharedToken(props.token)) {
      await initWorkspace();
      await navigateByPath('/');
    } else {
      // todo #117 set login error and update ui accordingly
    }
  } catch (e) {
    // todo #117: handle communication exception and update ui accordingly
    console.log('error', e);
  }
});
</script>
