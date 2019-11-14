<template>
  <div />
</template>

<script>
  import { setupApp } from '@/services/app-services';
  import api from '@/services/api';

  export default {
    name: 'LoginByLink',

    props: {
      token: String,
    },

    async created() {
      if (!this.token) {
        this.$route.push('/');
      } else {
        try {
          if (await api.tryAutoLogin()) {
            const sharedWorkspaceResponse = await api.post('/shared-workspaces', {
              token: this.token,
            });
            const workspace = sharedWorkspaceResponse.data;

            // todo #90: do not commit directly, use wrapper action
            this.$store.commit('workspaces/setCurrentWorkspace', workspace);

            await setupApp(this.$store);
            await this.$router.push('/');
          } else if (await api.loginBySharedToken(this.token)) {
            await setupApp(this.$store);
            await this.$router.push('/');
          } else {
          // todo #117 set login error and update ui accordingly
          }
        } catch (e) {
          // todo #117: handle communication exception and update ui accordingly
          console.log('error', e);
        }
      }
    },
  };
</script>
