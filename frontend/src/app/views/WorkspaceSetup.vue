<template>
  <el-container>
    <el-main>
      <h1>Workspace</h1>
      <el-form ref="form"
               :model="form"
               :rules="formValidationRules">
        <el-form-item label="Name" prop="name">
          <el-input v-model="form.name"></el-input>
        </el-form-item>
        <el-form-item label="Tax Enabled" prop="taxEnabled">
          <el-checkbox v-model="form.taxEnabled"></el-checkbox>
        </el-form-item>
        <el-form-item label="Multi-currency Enabled" prop="multiCurrencyEnabled">
          <el-checkbox v-model="form.multiCurrencyEnabled"></el-checkbox>
        </el-form-item>
        <el-form-item label="Default Currency" prop="defaultCurrency">
          <el-input v-model="form.defaultCurrency"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="save">Save</el-button>
        </el-form-item>
      </el-form>
    </el-main>
  </el-container>
</template>

<script>

  import api from '@/services/api'
  import {mapMutations} from 'vuex'

  export default {
    name: 'WorkspaceSetup',

    data: function () {
      return {
        form: {
          name: '',
          taxEnabled: false,
          multiCurrencyEnabled: false,
          defaultCurrency: 'AUD'
        },
        formValidationRules: {
          name: [
            {required: true, message: 'Please input name', trigger: 'blur'}
          ],
          defaultCurrency: [
            {required: true, message: 'Please input currency', trigger: 'blur'}
          ]
        }
      }
    },

    methods: {
      save: function () {
        this.$refs.form.validate((valid) => {
          if (valid) {
            api
                .post('/user/workspaces', this.form)
                .then(response => {
                  console.log(response)
                  this.createWorkspace(response.data)
                  this.$router.push('/')
                })
                .catch(() => {
                  this.$refs.form.clearValidate()
                  this.$message({
                    showClose: true,
                    message: 'Sorry, failed',
                    type: 'error'
                  });
                })
          }
        })
      },

      ...mapMutations({
        createWorkspace: 'workspaces/createWorkspace'
      })
    }
  }
</script>