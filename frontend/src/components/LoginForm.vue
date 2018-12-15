<template>
  <el-container>
    <el-main>
      <h1>Login page</h1>
      <el-form ref="form"
               :model="form"
               :rules="formValidationRules"
               label-width="120px">
        <el-form-item label="Username" prop="userName">
          <el-input v-model="form.userName"></el-input>
        </el-form-item>
        <el-form-item label="Password" prop="password">
          <el-input type="password" v-model="form.password"></el-input>
        </el-form-item>
        <el-form-item label="Remember me" prop="rememberMe">
          <el-checkbox v-model="form.rememberMe"></el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="login">Login</el-button>
        </el-form-item>
      </el-form>
    </el-main>
  </el-container>
</template>

<script>

  import api from '@/services/api'

  export default {
    name: 'LoginForm',

    data: function () {
      return {
        form: {
          userName: '',
          password: '',
          rememberMe: false
        },
        formValidationRules: {
          userName: [
            {required: true, message: 'Please input login', trigger: 'blur'}
          ],
          password: [
            {required: true, message: 'Please input password', trigger: 'blur'}
          ]
        }
      }
    },

    created: async function () {
      try {
        await api.tryAutoLogin()
        this.$emit('login')
      }
      catch (e) {
        // no opt, continue with the form login
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