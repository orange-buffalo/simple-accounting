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
        <el-form-item>
          <el-button type="primary" @click="login">Login</el-button>
        </el-form-item>
      </el-form>
    </el-main>
  </el-container>
</template>

<script>

  import api from '@/services/api'
  import {mapMutations} from 'vuex'

  export default {
    name: 'login',

    data: function () {
      return {
        form: {
          userName: '',
          password: '',
          valid: false
        },
        formValidationRules: {
          userName: [
            {required: true, message: 'Please input Activity name', trigger: 'blur'}
          ],
          password: [
            {required: true, message: 'Please input Activity name', trigger: 'blur'}
          ]
        }
      }
    },

    methods: {
      ...mapMutations({
        updateJwtToken: 'api/updateJwtToken'
      }),

      login: function () {
        this.$refs.form.validate((valid) => {
          if (valid) {
            api
                .post('/auth/login', {
                  userName: this.form.userName,
                  password: this.form.password,
                  rememberMe: false
                })
                .then(response => {
                  this.updateJwtToken(response.data.token)
                  this.$router.push('/')
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