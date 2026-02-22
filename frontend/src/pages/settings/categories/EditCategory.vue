<template>
  <div>
    <div class="sa-page-header">
      <h1>Edit Category</h1>
    </div>

    <SaLegacyForm
      ref="formRef"
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
  import type { PartialBy } from '@/services/utils';
  import type { EditCategoryDto } from '@/services/api';
  import { categoriesApi } from '@/services/api';
  import useNavigation from '@/services/use-navigation';
  import { useForm } from '@/components/form/use-form';

  const props = defineProps<{
    id: number,
  }>();

  const { navigateByViewName } = useNavigation();
  const navigateToCategoriesOverview = async () => navigateByViewName('settings-categories');

  type CategoryFormValues = PartialBy<EditCategoryDto, 'name'>;
  const category = ref<CategoryFormValues>({
    income: false,
    expense: false,
  });

  const { currentWorkspaceId } = useCurrentWorkspace();

  const loadCategory = async () => {
    category.value = await categoriesApi.getCategory({
      categoryId: props.id,
      workspaceId: currentWorkspaceId,
    });
  };

  const saveCategory = async () => {
    await categoriesApi.updateCategory({
      categoryId: props.id,
      editCategoryDto: category.value as EditCategoryDto,
      workspaceId: currentWorkspaceId,
    });
    await navigateToCategoriesOverview();
  };

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

  const {
    formRef,
    submitForm,
  } = useForm(loadCategory, saveCategory);
</script>
