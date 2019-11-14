<template>
  <div>
    <ElCard>
      <ElForm
        ref="categoryForm"
        :model="category"
        :rules="categoryValidationRules"
      >
        <ElFormItem
          label="Name"
          prop="name"
        >
          <ElInput v-model="category.name" />
        </ElFormItem>
        <ElFormItem
          label="Description"
          prop="description"
        >
          <ElInput
            v-model="category.description"
            type="textarea"
          />
        </ElFormItem>
        <ElFormItem
          label="Income"
          prop="income"
        >
          <ElCheckbox v-model="category.income" />
        </ElFormItem>
        <ElFormItem
          label="Expense"
          prop="expense"
        >
          <ElCheckbox v-model="category.expense" />
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
    </ElCard>
  </div>
</template>

<script>

  import { mapMutations, mapState } from 'vuex';
  import api from '@/services/api';

  export default {
    name: 'CreateCategory',

    data() {
      return {
        category: {
          name: null,
          description: null,
          income: false,
          expense: false,
        },
        categoryValidationRules: {
          name: [
            { required: true, message: 'Please input name', trigger: 'blur' },
          ],
          defaultCurrency: [
            { required: true, message: 'Please input currency', trigger: 'blur' },
          ],
          income: [
            {
              validator: (rule, value, callback) => {
                if (!this.category.income && !this.category.expense) {
                  callback(new Error('At least one of income/expense must be selected'));
                } else {
                  callback();
                }
              },
            },
          ],
        },
      };
    },

    computed: {
      ...mapState('workspaces', {
        workspace: 'currentWorkspace',
      }),
    },

    methods: {
      save() {
        this.$refs.categoryForm.validate((valid) => {
          if (valid) {
            api
              .post(`/workspaces/${this.workspace.id}/categories`, this.category)
              .then(() => {
                this.$router.push({ name: 'settings-categories' });
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
    },
  };
</script>
