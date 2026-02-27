<template>
  <SaStatusLabel
    v-if="status === 'LOADING'"
    status="regular"
    custom-icon="loading"
  >
    {{ $t.loginByLinkPage.loading() }}
  </SaStatusLabel>

  <SaStatusLabel
    v-if="status === 'ERROR'"
    status="failure"
  >
    {{ $t.loginByLinkPage.error() }}
  </SaStatusLabel>

  <SaStatusLabel
    v-if="status === 'SUCCESS'"
    status="success"
  >
    {{ $t.loginByLinkPage.success() }}
  </SaStatusLabel>
</template>

<script lang="ts" setup>
  import { onMounted, ref } from 'vue';
  import { initWorkspace, useWorkspaces } from '@/services/workspaces';
  import useNavigation from '@/services/use-navigation';
  import { useAuth, workspacesApi } from '@/services/api';
  import { $t } from '@/services/i18n';
  import SaStatusLabel from '@/components/SaStatusLabel.vue';

  const props = defineProps<{
    token?: string,
  }>();

  type Status = 'LOADING' | 'ERROR' | 'SUCCESS';
  const status = ref<Status>('LOADING');

  const { navigateByPath } = useNavigation();

  onMounted(async () => {
    if (props.token === undefined) {
      await navigateByPath('/');
      return;
    }
    const {
      isLoggedIn,
      loginBySharedToken,
    } = useAuth();

    try {
      let loginSuccessful = false;
      if (isLoggedIn()) {
        const workspace = await workspacesApi.saveSharedWorkspace({
          saveSharedWorkspaceRequestDto: {
            token: props.token,
          },
        });

        const {
          loadWorkspaces,
          setCurrentWorkspace,
        } = useWorkspaces();
        setCurrentWorkspace(workspace);
        await loadWorkspaces();
        loginSuccessful = true;
      } else if (await loginBySharedToken(props.token)) {
        await initWorkspace();
        loginSuccessful = true;
      }

      if (loginSuccessful) {
        status.value = 'SUCCESS';
        setTimeout(() => navigateByPath('/'), 1000);
      } else {
        status.value = 'ERROR';
      }
    } catch (e) {
      status.value = 'ERROR';
    }
  });
</script>
