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
import { useForm } from '@/components/form/use-form';
import type { CreateCategoryDto } from '@/services/api';
import { categoriesApi } from '@/services/api';
import useNavigation from '@/services/use-navigation';
import type { PartialBy } from '@/services/utils';
import { useCurrentWorkspace } from '@/services/workspaces';

type CategoryFormValues = PartialBy<CreateCategoryDto, 'name'>;
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
  defaultCurrency: [
    {
      required: true,
      message: 'Please input currency',
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
const navigateToCategoriesOverview = async () => navigateByViewName('/settings/categories');

const { currentWorkspaceId } = useCurrentWorkspace();
const saveCategory = async () => {
  await categoriesApi.createCategory({
    createCategoryDto: category.value as CreateCategoryDto,
    workspaceId: currentWorkspaceId,
  });
  await navigateToCategoriesOverview();
};

const { formRef, submitForm } = useForm(async () => {
  // no op
}, saveCategory);
</script>
