<template>
  <ElContainer>
    <ElMain>
      <h1>Workspace</h1>
      <ElForm
        ref="form"
        :model="form"
        :rules="formValidationRules"
      >
        <ElFormItem
          label="Name"
          prop="name"
        >
          <ElInput v-model="form.name" />
        </ElFormItem>
        <ElFormItem
          label="Tax Enabled"
          prop="taxEnabled"
        >
          <ElCheckbox v-model="form.taxEnabled" />
        </ElFormItem>
        <ElFormItem
          label="Multi-currency Enabled"
          prop="multiCurrencyEnabled"
        >
          <ElCheckbox v-model="form.multiCurrencyEnabled" />
        </ElFormItem>
        <ElFormItem
          label="Default Currency"
          prop="defaultCurrency"
        >
          <ElInput v-model="form.defaultCurrency" />
        </ElFormItem>
        <ElFormItem>
          <ElButton
            type="primary"
            @click="save"
          >
            Save
          </ElButton>
        </ElFormItem>
      </ElForm>
    </ElMain>
  </ElContainer>
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
