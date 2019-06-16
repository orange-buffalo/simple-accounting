<template>
  <el-container>
    <el-main>
      <h1>New user</h1>
      <el-form ref="form"
               :model="form"
               :rules="formValidationRules">
        <el-form-item label="Username" prop="userName">
          <el-input v-model="form.userName"></el-input>
        </el-form-item>
        <el-form-item label="Password" prop="password">
          <el-input type="password" v-model="form.password"></el-input>
        </el-form-item>
        <el-form-item label="Confirm Password" prop="confirmPassword">
          <el-input type="password" v-model="form.confirmPassword"></el-input>
        </el-form-item>
        <el-form-item label="Admin?" prop="admin">
          <el-checkbox v-model="form.admin"></el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="saveUser">Save</el-button>
        </el-form-item>
      </el-form>
    </el-main>
  </el-container>
</template>

<script>

  import api from '@/services/api'
  import {mapMutations} from 'vuex'

  export default {
    name: 'CreateUser',

    data: function () {
      return {
        form: {
          userName: '',
          password: '',
          confirmPassword: '',
          admin: false
        },
        formValidationRules: {
          userName: [
            {required: true, message: 'Please input Activity name', trigger: 'blur'}
          ],
          password: [
            {required: true, message: 'Please input Activity name', trigger: 'blur'}
          ],
          confirmPassword: [
            {required: true, message: 'Please input Activity name', trigger: 'blur'},
            {
              validator: (rule, value, callback) => {
                if (value !== this.form.password) {
                  callback(new Error('Two inputs don\'t match!'));
                }
                else {
                  callback();
                }
              },
              trigger: 'blur'
            }
          ]
        }
      }
    },

    methods: {

      saveUser: function () {
        this.$refs.form.validate((valid) => {
          if (valid) {
            console.log('saving')
            api
                .post('/users', {
                  userName: this.form.userName,
                  password: this.form.password,
                  admin: this.form.admin
                })
                .then(response => {
                  this.$router.push('/users')
                })
          }
        })
      }
    }
  }
</script>