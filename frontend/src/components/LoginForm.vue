<template>
  <el-form ref="form"
           class="login-form"
           :model="form"
           :rules="formValidationRules"
           label-width="0px">
    <el-form-item prop="userName">
      <el-input v-model="form.userName"
                placeholder="Login">
        <svgicon name="login" slot="prefix"/>
      </el-input>
    </el-form-item>

    <el-form-item prop="password">
      <el-input type="password"
                v-model="form.password"
                placeholder="Password">
        <svgicon name="password" slot="prefix"/>
      </el-input>
    </el-form-item>

    <el-form-item prop="rememberMe" align="center">
      <el-checkbox v-model="form.rememberMe">Remember me</el-checkbox>
    </el-form-item>

    <el-button type="primary"
               @click="login"
               :disabled="!loginEnabled">Login
    </el-button>
  </el-form>
</template>

<script>

  import api from '@/services/api'
  import '@/components/icons/login'
  import '@/components/icons/password'

  export default {
    name: 'LoginForm',

    data: function () {
      return {
        form: {
          userName: '',
          password: '',
          rememberMe: true
        }
      }
    },

    created: async function () {
      try {
        await api.tryAutoLogin()
        this.$emit('login')
      } catch (e) {
        // no opt, continue with the form login
      }
    },

    computed: {
      loginEnabled: function () {
        return this.form.userName && this.form.password;
      }
    },

    methods: {
      login: function () {
        this.$refs.form.validate((valid) => {
          if (valid) {
            api
                .login({
                  userName: this.form.userName,
                  password: this.form.password,
                  rememberMe: this.form.rememberMe
                })
                .then(() => {
                  this.$emit('login')
                })
                .catch(() => {
                  this.$refs.form.clearValidate()
                  this.$message({
                    showClose: true,
                    message: 'Login failed',
                    type: 'error'
                  });
                })
          }
        })
      }
    }
  }
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