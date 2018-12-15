<template>
  <login-form @login="onLogin"/>
</template>

<script>
  import {setupApp} from '@/app/services/app-services'
  import LoginForm from '@/components/LoginForm'

  export default {
    name: 'Login',

    components: {
      LoginForm
    },

    methods: {
      onLogin: function () {
        setupApp(this.$store, this.$router).then(() => {
          if (!this.$store.state.workspaces.currentWorkspace) {
            this.$router.push('/workspace-setup')
          } else {
            if (this.$store.state.app.lastView) {
              this.$router.push({name: this.$store.state.app.lastView})
            } else {
              this.$router.push('/')
            }
          }
        })
      }
    }
  }
</script>