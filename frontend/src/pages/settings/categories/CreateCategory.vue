<template>
  <div>
    <div class="sa-page-header">
      <h1>Create New Category</h1>
    </div>

    <SaLegacyForm
      ref="formRef"
      :model="category"
      :rules="categoryValidationRules"
      :initially-loading="false"
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
        <ElFormItem prop="income">
          <ElCheckbox v-model="category.income">Income</ElCheckbox>
        </ElFormItem>
        <ElFormItem prop="expense">
          <ElCheckbox v-model="category.expense">Expense</ElCheckbox>
        </ElFormItem>
      </template>

      <template #buttons-bar>
        <ElButton @click="navigateToCategoriesOverview">
          Cancel
        </ElButton>
        <ElButton
          type="primary"
          @click="submitForm"
        >
          Save
        </ElButton>
      </template>
    </SaLegacyForm>
  </div>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import SaLegacyForm from '@/components/form/SaLegacyForm.vue';
  import { useCurrentWorkspace } from '@/services/workspaces';
  import useNavigation from '@/services/use-navigation';
  import { useForm } from '@/components/form/use-form';
  import { graphql } from '@/services/api/gql';
  import { useMutation } from '@/services/api/use-gql-api.ts';

  type CategoryFormValues = {
    name?: string,
    description?: string,
    income: boolean,
    expense: boolean,
  };
  const category = ref<CategoryFormValues>({
    income: false,
    expense: false,
  });

  const categoryValidationRules = {
    name: [
      {
        required: true,
        message: 'Please input name',
        trigger: 'blur',
      },
    ],
    income: [
      {
        validator: (rule: unknown, value: unknown, callback: (error?: Error) => void) => {
          if (!category.value.income && !category.value.expense) {
            callback(new Error('At least one of income/expense must be selected'));
          } else {
            callback();
          }
        },
      },
    ],
  };

  const { navigateByViewName } = useNavigation();
  const navigateToCategoriesOverview = async () => navigateByViewName('settings-categories');

  const { currentWorkspaceId } = useCurrentWorkspace();

  const createCategoryMutation = useMutation(graphql(`
    mutation createCategoryMutation(
      $workspaceId: Long!,
      $name: String!,
      $description: String,
      $income: Boolean!,
      $expense: Boolean!
    ) {
      createCategory(
        workspaceId: $workspaceId,
        name: $name,
        description: $description,
        income: $income,
        expense: $expense
      ) {
        id
      }
    }
  `), 'createCategory');

  const saveCategory = async () => {
    await createCategoryMutation({
      workspaceId: currentWorkspaceId,
      name: category.value.name!,
      description: category.value.description ?? null,
      income: category.value.income,
      expense: category.value.expense,
    });
    await navigateToCategoriesOverview();
  };

  const {
    formRef,
    submitForm,
  } = useForm(async () => {
    // no op
  }, saveCategory);
</script>
