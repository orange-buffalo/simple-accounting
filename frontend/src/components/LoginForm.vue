<template>
  <el-form
    ref="form"
    class="login-form"
    :model="form"
    :rules="formValidationRules"
    label-width="0px"
  >
    <el-form-item prop="userName">
      <el-input
        v-model="form.userName"
        placeholder="Login"
      >
        <svgicon
          slot="prefix"
          name="login"
        />
      </el-input>
    </el-form-item>

    <el-form-item prop="password">
      <el-input
        v-model="form.password"
        type="password"
        placeholder="Password"
      >
        <svgicon
          slot="prefix"
          name="password"
        />
      </el-input>
    </el-form-item>

    <el-form-item
      prop="rememberMe"
      align="center"
    >
      <el-checkbox v-model="form.rememberMe">
        Remember me
      </el-checkbox>
    </el-form-item>

    <el-button
      type="primary"
      :disabled="!loginEnabled"
      @click="login"
    >
      Login
    </el-button>
  </el-form>
</template>

<script>

  import api from '@/services/api';
  import '@/components/icons/login';
  import '@/components/icons/password';

  export default {
    name: 'LoginForm',

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
      if (await api.tryAutoLogin()) {
        this.$emit('login');
      }
    },

    methods: {
      login() {
        this.$refs.form.validate((valid) => {
          if (valid) {
            api
              .login({
                userName: this.form.userName,
                password: this.form.password,
                rememberMe: this.form.rememberMe,
              })
              .then(() => {
                this.$emit('login');
              })
              .catch(() => {
                this.$refs.form.clearValidate();
                this.$message({
                  showClose: true,
                  message: 'Login failed',
                  type: 'error',
                });
              });
          }
        });
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
