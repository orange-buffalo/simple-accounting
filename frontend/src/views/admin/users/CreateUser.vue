<template>
  <ElContainer>
    <ElMain>
      <h1>New user</h1>
      <ElForm
        ref="form"
        :model="form"
        :rules="formValidationRules"
      >
        <ElFormItem
          label="Username"
          prop="userName"
        >
          <ElInput v-model="form.userName" />
        </ElFormItem>
        <ElFormItem
          label="Password"
          prop="password"
        >
          <ElInput
            v-model="form.password"
            type="password"
          />
        </ElFormItem>
        <ElFormItem
          label="Confirm Password"
          prop="confirmPassword"
        >
          <ElInput
            v-model="form.confirmPassword"
            type="password"
          />
        </ElFormItem>
        <ElFormItem
          label="Admin?"
          prop="admin"
        >
          <ElCheckbox v-model="form.admin" />
        </ElFormItem>
        <ElFormItem>
          <ElButton
            type="primary"
            @click="saveUser"
          >
            Save
          </ElButton>
        </ElFormItem>
      </ElForm>
    </ElMain>
  </ElContainer>
</template>

<script>
  import { api } from '@/services/api';

  export default {
    name: 'CreateUser',

    data() {
      return {
        form: {
          userName: '',
          password: '',
          confirmPassword: '',
          admin: false,
        },
        formValidationRules: {
          userName: [
            { required: true, message: 'Please input Activity name', trigger: 'blur' },
          ],
          password: [
            { required: true, message: 'Please input Activity name', trigger: 'blur' },
          ],
          confirmPassword: [
            { required: true, message: 'Please input Activity name', trigger: 'blur' },
            {
              validator: (rule, value, callback) => {
                if (value !== this.form.password) {
                  callback(new Error('Two inputs don\'t match!'));
                } else {
                  callback();
                }
              },
              trigger: 'blur',
            },
          ],
        },
      };
    },

    methods: {

      saveUser() {
        this.$refs.form.validate((valid) => {
          if (valid) {
            api
              .post('/users', {
                userName: this.form.userName,
                password: this.form.password,
                admin: this.form.admin,
              })
              .then(() => {
                this.$router.push('/users');
              });
          }
        });
      },
    },
  };
</script>
