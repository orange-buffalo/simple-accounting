<template>
  <div />
</template>

<script>
  import { api } from '@/services/api-legacy';
  import { initWorkspace } from '@/services/workspaces-service';

  export default {
    name: 'LoginByLink',

    props: {
      token: {
        type: String,
        required: true,
      },
    },

    async created() {
      if (!this.token) {
        this.$route.push('/');
      } else {
        try {
          if (api.isLoggedIn()) {
            const sharedWorkspaceResponse = await api.post('/shared-workspaces', {
              token: this.token,
            });
            const workspace = sharedWorkspaceResponse.data;

            // todo #90: do not commit directly, use wrapper action
            this.$store.commit('workspaces/setCurrentWorkspace', workspace);

            await initWorkspace();
            await this.$router.push('/');
          } else if (await api.loginBySharedToken(this.token)) {
            await initWorkspace();
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
