<template>
  <div class="login-page">
    <div class="login-page__signup">
      New here? We are launching public access soon.
    </div>
    <div class="login-page__login">
      <div class="login-page__login__logo" />
      <LoginForm @login="onLogin" />
    </div>
  </div>
</template>

<script>
  import { setupApp } from '@/services/app-services';
  import LoginForm from '@/components/LoginForm';

  export default {
    name: 'Login',

    components: {
      LoginForm,
    },

    methods: {
      async onLogin() {
        if (this.$store.state.api.isAdmin) {
          this.$router.push({ name: 'users-overview' });
        } else {
          await setupApp(this.$store, this.$router);
          if (!this.$store.state.workspaces.currentWorkspace) {
            this.$router.push('/workspace-setup');
          } else if (this.$store.state.app.lastView) {
            this.$router.push({ name: this.$store.state.app.lastView });
          } else {
            this.$router.push('/');
          }
        }
      },
    },
  };
</script>

<style lang="scss">
  @import "~@/styles/vars.scss";
  @import "~@/styles/mixins.scss";

  .login-page {
    display: flex;
    height: 100vh;

    &__signup {
      @include gradient-background;
      width: 30%;
      display: flex;
      align-items: center;
      justify-content: space-around;
      color: white;
      font-size: 110%;
    }

    &__login {
      width: 70%;
      background-color: white;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;

      .el-form {
        min-width: 40%;

        .el-button--primary {
          background: $primary-color-lighter-iii;
          border-color: $primary-color-lighter-iii;
          transition: all 0.25s;

          &:hover {
            background: $primary-color-lighter-ii;
            border-color: $primary-color-lighter-ii;
          }

          &.is-disabled {
            background: white;
            color: $primary-color-lighter-iii;
            cursor: inherit;

            &:hover {
              border-color: $primary-color-lighter-iii;
            }
          }
        }

        .el-checkbox__label {
          color: $primary-color-lighter-ii !important;
        }

        .el-checkbox__inner {
          background-color: white !important;
          border-color: $primary-color-lighter-ii !important;

          &::after {
            border-color: $primary-color-lighter-ii;
          }
        }

        .el-input__inner {
          border-color: $primary-color-lighter-iii;

          &:focus {
            border-color: $primary-color-lighter-iii;
          }
        }
      }

      &__logo {
        height: 150px;
        width: 150px;
        background: url("../assets/logo-login.svg");
        background-size: contain;
        background-repeat: no-repeat;
        background-position: center;
        margin-bottom: 40px;
      }
    }
  }

</style>
