<template>
  <div>
    <el-card>
      <el-form
        ref="categoryForm"
        :model="category"
        :rules="categoryValidationRules"
      >
        <el-form-item
          label="Name"
          prop="name"
        >
          <el-input v-model="category.name" />
        </el-form-item>
        <el-form-item
          label="Description"
          prop="description"
        >
          <el-input
            v-model="category.description"
            type="textarea"
          />
        </el-form-item>
        <el-form-item
          label="Income"
          prop="income"
        >
          <el-checkbox v-model="category.income" />
        </el-form-item>
        <el-form-item
          label="Expense"
          prop="expense"
        >
          <el-checkbox v-model="category.expense" />
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
    </el-card>
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
