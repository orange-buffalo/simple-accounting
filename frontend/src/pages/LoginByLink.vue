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
  import { useWorkspaces } from '@/services/workspaces';
  import useNavigation from '@/services/use-navigation';
  import { useAuth } from '@/services/api';
  import { $t } from '@/services/i18n';
  import SaStatusLabel from '@/components/SaStatusLabel.vue';
  import { graphql } from '@/services/api/gql';
  import { useMutation } from '@/services/api/use-gql-api';

  const props = defineProps<{
    token?: string,
  }>();

  type Status = 'LOADING' | 'ERROR' | 'SUCCESS';
  const status = ref<Status>('LOADING');

  const { navigateByPath } = useNavigation();

  const saveSharedWorkspaceMutation = graphql(`
    mutation saveSharedWorkspaceLoginByLink($token: String!) {
      saveSharedWorkspace(token: $token) {
        id
        name
        defaultCurrency
      }
    }
  `);

  const executeSaveSharedWorkspace = useMutation(saveSharedWorkspaceMutation, 'saveSharedWorkspace');

  onMounted(async () => {
    if (props.token === undefined) {
      await navigateByPath('/');
      return;
    }
    const {
      isLoggedIn,
      loginBySharedToken,
    } = useAuth();
    const {
      loadWorkspaces,
      setCurrentWorkspace,
    } = useWorkspaces();

    try {
      let loginSuccessful = false;
      if (isLoggedIn()) {
        const sharedWorkspace = await executeSaveSharedWorkspace({ token: props.token });
        setCurrentWorkspace({ ...sharedWorkspace, editable: false });
        await loadWorkspaces();
        loginSuccessful = true;
      } else {
        const workspace = await loginBySharedToken(props.token);
        if (workspace) {
          setCurrentWorkspace({ ...workspace, editable: false });
          loginSuccessful = true;
        }
      }

      if (loginSuccessful) {
        status.value = 'SUCCESS';
        setTimeout(() => navigateByPath('/'), 1000);
      } else {
        status.value = 'ERROR';
      }
    } catch (_e) {
      status.value = 'ERROR';
    }
  });
</script>
