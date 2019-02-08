<template>
  <div class="login-page">
    <div class="login-page__signup">
      New here? We are launching public access soon.
    </div>
    <div class="login-page__login">

      <login-form @login="onLogin"/>
    </div>
  </div>
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

<style lang="scss">
  @import "@/app/styles/vars.scss";
  @import "@/app/styles/mixins.scss";

  .login-page {
    display: flex;
    height: 100vh;
  }

  .login-page__signup {
    @include gradient-background;
    width: 30%;
    display: flex;
    align-items: center;
    justify-content: space-around;
    color: white;
    font-size: 110%;
  }

  .login-page__login {
    width: 70%;
    background-color: white;
    display: flex;
    align-items: center;
    justify-content: space-around;

    .el-form {
      min-width: 40%;
    }
  }

</style>