<template>
  <div>
    <div class="sa-page-header">
      <h1>Create New Category</h1>
    </div>

    <SaForm
      ref="categoryForm"
      :model="category"
      :rules="categoryValidationRules"
    >
      <template #default>
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
      </template>

      <template #buttons-bar>
        <ElButton @click="navigateToCategoriesOverview">
          Cancel
        </ElButton>
        <ElButton
          type="primary"
          @click="save"
        >
          Save
        </ElButton>
      </template>
    </SaForm>
  </div>
</template>

<script>
  import { api } from '@/services/api';
  import SaForm from '@/components/SaForm';
  import withWorkspaces from '@/components/mixins/with-workspaces';

  export default {
    name: 'CreateCategory',

    components: { SaForm },

    mixins: [withWorkspaces],

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
            {
              required: true,
              message: 'Please input name',
              trigger: 'blur',
            },
          ],
          defaultCurrency: [
            {
              required: true,
              message: 'Please input currency',
              trigger: 'blur',
            },
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

    methods: {
      async save() {
        try {
          await this.$refs.categoryForm.validate();
        } catch (e) {
          return;
        }

        await api.post(`/workspaces/${this.currentWorkspace.id}/categories`, this.category);
        await this.$router.push({ name: 'settings-categories' });
      },

      navigateToCategoriesOverview() {
        this.$router.push({ name: '/settings/categories' });
      },
    },
  };
</script>
