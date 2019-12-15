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
  import { api } from '@/services/api';
  import LoginForm from '@/components/LoginForm';
  import { initWorkspace } from '@/services/workspaces-service';
  import { userApi } from '@/services/user-api';
  import { app } from '@/services/app-services';

  export default {
    name: 'Login',

    components: {
      LoginForm,
    },

    methods: {
      async onLogin() {
        if (api.isAdmin()) {
          await this.$router.push({ name: 'users-overview' });
        } else {
          const profile = await userApi.getProfile();
          await app.i18n.setLocaleFromProfile(profile.i18n);

          await initWorkspace();

          if (!this.$store.state.workspaces.currentWorkspace) {
            await this.$router.push('/workspace-setup');
          } else if (this.$store.state.app.lastView) {
            await this.$router.push({ name: this.$store.state.app.lastView });
          } else {
            await this.$router.push('/');
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
