<template>
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
</template>

<script>

  import { api } from '@/services/api';
  import SaIcon from '@/components/SaIcon';

  export default {
    name: 'LoginForm',
    components: { SaIcon },
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
          this.$emit('login');
        } catch ({ response: { data } }) {
          if (data && data.error === 'AccountLocked') {
            this.$message({
              showClose: true,
              // todo #98: localize and humanize
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
    },
  };
</script>

<style lang="scss">
  .login-form {
    .el-button {
      width: 100%;
      padding: 15px;
      text-transform: uppercase;
      font-weight: bold;
    }

    .svg-icon {
      margin-left: 5px;
      margin-top: -3px;
    }

    .el-input--prefix .el-input__inner {
      padding-left: 32px;
    }
  }
</style>
