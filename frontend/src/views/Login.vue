<template>
  <div class="login-page">
    <div class="login-page__signup">
      New here? We are launching public access soon.
    </div>
    <div class="login-page__login">
      <LogoLogin class="login-page__login__logo" />

      <ElForm
        ref="form"
        class="login-form"
        :model="form"
        label-width="0px"
      >
        <ElFormItem prop="userName">
          <ElInput
            v-model="form.userName"
            :placeholder="$t('loginPage.userName.placeholder')"
          >
            <SaIcon
              slot="prefix"
              icon="login"
            />
          </ElInput>
        </ElFormItem>

        <ElFormItem prop="password">
          <ElInput
            v-model="form.password"
            type="password"
            placeholder="Password"
          >
            <SaIcon
              slot="prefix"
              icon="password"
            />
          </ElInput>
        </ElFormItem>

        <ElFormItem
          prop="rememberMe"
          align="center"
        >
          <ElCheckbox v-model="form.rememberMe">
            Remember me
          </ElCheckbox>
        </ElFormItem>

        <ElButton
          type="primary"
          :disabled="!loginEnabled"
          @click="login"
        >
          Login
        </ElButton>
      </ElForm>
    </div>
  </div>
</template>

<script>
  import { api } from '@/services/api';
  import { initWorkspace } from '@/services/workspaces-service';
  import { userApi } from '@/services/user-api';
  import { app } from '@/services/app-services';
  import LogoLogin from '@/assets/logo-login.svg';
  import SaIcon from '@/components/SaIcon';

  export default {
    components: {
      SaIcon,
      LogoLogin,
    },

    data() {
      return {
        form: {
          userName: '',
          password: '',
          rememberMe: true,
        },
      };
    },

    computed: {
      loginEnabled() {
        return this.form.userName && this.form.password;
      },
    },

    async created() {
      if (api.isLoggedIn()) {
        this.$emit('login');
      }
    },

    methods: {
      async login() {
        try {
          await api.login({
            userName: this.form.userName,
            password: this.form.password,
            rememberMe: this.form.rememberMe,
          });
          await this.onLogin();
        } catch ({ response: { data } }) {
          if (data && data.error === 'AccountLocked') {
            this.$message({
              showClose: true,
              // todo #115: localize and humanize
              message: `Account is locked for ${data.lockExpiresInSec} seconds`,
              type: 'error',
            });
          } else if (data && data.error === 'LoginNotAvailable') {
            this.$message({
              showClose: true,
              message: 'Looks like your account is under attack!',
              type: 'error',
            });
          } else {
            this.$message({
              showClose: true,
              message: 'Login failed',
              type: 'error',
            });
          }
        }
      },

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
        margin-bottom: 40px;
      }
    }
  }

  .login-form {
    .el-button {
      width: 100%;
      padding: 15px;
      text-transform: uppercase;
      font-weight: bold;
    }

    .sa-icon {
      margin-left: 5px;
      margin-top: -3px;
    }

    .el-input--prefix .el-input__inner {
      padding-left: 32px;
    }
  }

</style>
