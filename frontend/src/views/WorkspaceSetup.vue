<template>
  <el-container>
    <el-main>
      <h1>Workspace</h1>
      <el-form
        ref="form"
        :model="form"
        :rules="formValidationRules"
      >
        <el-form-item
          label="Name"
          prop="name"
        >
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item
          label="Tax Enabled"
          prop="taxEnabled"
        >
          <el-checkbox v-model="form.taxEnabled" />
        </el-form-item>
        <el-form-item
          label="Multi-currency Enabled"
          prop="multiCurrencyEnabled"
        >
          <el-checkbox v-model="form.multiCurrencyEnabled" />
        </el-form-item>
        <el-form-item
          label="Default Currency"
          prop="defaultCurrency"
        >
          <el-input v-model="form.defaultCurrency" />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            @click="save"
          >
            Save
          </el-button>
        </el-form-item>
      </el-form>
    </el-main>
  </el-container>
</template>

<script>

  import { mapActions } from 'vuex';
  import api from '@/services/api';

  export default {
    name: 'WorkspaceSetup',

    data() {
      return {
        form: {
          name: '',
          taxEnabled: false,
          multiCurrencyEnabled: false,
          defaultCurrency: 'AUD',
        },
        formValidationRules: {
          name: [
            { required: true, message: 'Please input name', trigger: 'blur' },
          ],
          defaultCurrency: [
            { required: true, message: 'Please input currency', trigger: 'blur' },
          ],
        },
      };
    },

    methods: {
      save() {
        this.$refs.form.validate((valid) => {
          if (valid) {
            api
              .post('/workspaces', this.form)
              .then((response) => {
                console.log(response);
                this.createWorkspace(response.data);
                this.$router.push('/');
              })
              .catch(() => {
                this.$refs.form.clearValidate();
                this.$message({
                  showClose: true,
                  message: 'Sorry, failed',
                  type: 'error',
                });
              });
          }
        });
      },

      ...mapActions({
        createWorkspace: 'workspaces/createWorkspace',
      }),
    },
  };
</script>
