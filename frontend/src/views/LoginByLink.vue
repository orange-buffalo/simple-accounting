<template>
  <div>
  </div>
</template>

<script>
  import {setupApp} from '@/services/app-services'
  import api from '@/services/api'

  export default {
    name: 'LoginByLink',

    props: {
      token: String
    },

    created: async function () {
      if (!this.token) {
        this.$route.push('/')
      } else {
        try {
          await api.tryAutoLogin()

          let sharedWorkspaceResponse = await api.post('/shared-workspaces', {
            token: this.token
          })
          let sharedWorkspace = sharedWorkspaceResponse.data

          // todo #90: do not commit directly, use wrapper action
          this.$store.commit('workspaces/setCurrentWorkspace', sharedWorkspace)

          await setupApp(this.$store)

          await this.$router.push('/')

        } catch (e) {
          // todo #111: for not logged in user, get access token using shared workspace token, via token-by-shared-workspace
          // todo #111: handle expiration, revocation, invalid token cases
          console.log('error', e)
        }
      }
    }
  }
</script>